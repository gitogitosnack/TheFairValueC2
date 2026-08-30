package org.example.web.batch.marketdata.client;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.example.web.batch.marketdata.client.dto.EdinetDocumentListItem;
import org.example.web.batch.marketdata.client.dto.FinancialStatementData;
import org.example.web.batch.marketdata.config.MarketDataApiProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// FinancialDataProvider の日本株向け実装。EDINET API v2（documents.json / documents/{docID}）を
// 呼び出す（設計書 5.1 参照）。実際の書類一覧取得・絞り込みは EdinetDocumentListTasklet が担うが、
// EDINET 呼び出し自体（documents.json の取得・documents/{docID} のダウンロード＋CSV代替データのパース）は
// このクラスが提供する fetchDocumentList() / fetchStatementDocument() を通じて行う（設計書 4.3.2 参照）。
@Component("edinetFinancialDataProvider")
public class EdinetFinancialDataProviderImpl implements FinancialDataProvider {

    // 有価証券報告書
    private static final String DOC_TYPE_YUHO = "120";
    // 半期報告書（2024年4月以後の四半期報告書廃止に伴う移行分を含む）
    private static final List<String> DOC_TYPE_HANKI = List.of("140", "160");

    private final RestClient restClient;
    private final MarketDataApiProperties apiProperties;

    public EdinetFinancialDataProviderImpl(
            RestClient.Builder marketDataRestClientBuilder,
            MarketDataApiProperties apiProperties) {
        this.apiProperties = apiProperties;
        this.restClient = marketDataRestClientBuilder.clone()
                .baseUrl(apiProperties.getEdinet().getBaseUrl())
                .build();
    }

    @Override
    public FinancialStatementData fetchLatestStatement(String code) {
        // EDINETは銘柄コード指定の単発取得APIを持たないため、このインタフェースメソッドは使用しない。
        // 書類一覧の取得・絞り込みは fetchDocumentList()（EdinetDocumentListTasklet 用）、
        // 書類本体の取得・パースは fetchStatementDocument()（JpFinancialStatementItemProcessor 用）
        // という2段構成で対応する（設計書 4.3.2 参照）。
        throw new UnsupportedOperationException(
                "EDINET has no per-code lookup API; use fetchDocumentList()/fetchStatementDocument() instead");
    }

    /**
     * 指定日に提出された書類一覧を取得し、有価証券報告書・半期報告書のみに絞り込んで返す。
     * その日 1 回だけ呼び出される想定（EdinetDocumentListTasklet 参照、設計書 4.3.2）。
     */
    public List<EdinetDocumentListItem> fetchDocumentList(LocalDate date) {
        EdinetDocumentListResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/documents.json")
                            .queryParam("date", date.toString())
                            .queryParam("type", "2")
                            .queryParam("Subscription-Key", apiProperties.getEdinet().getApiKey())
                            .build())
                    .retrieve()
                    .body(EdinetDocumentListResponse.class);
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new RateLimitException("EDINET documents.json rate limited: date=" + date, e);
        }
        if (response == null || response.results() == null) {
            return List.of();
        }
        List<EdinetDocumentListItem> items = new ArrayList<>();
        for (EdinetDocument doc : response.results()) {
            if (doc.docTypeCode() == null) {
                continue;
            }
            if (!DOC_TYPE_YUHO.equals(doc.docTypeCode()) && !DOC_TYPE_HANKI.contains(doc.docTypeCode())) {
                continue;
            }
            LocalDate periodEnd = (doc.periodEnd() != null && !doc.periodEnd().isBlank())
                    ? LocalDate.parse(doc.periodEnd())
                    : null;
            items.add(new EdinetDocumentListItem(doc.docID(), doc.secCode(), doc.docTypeCode(), periodEnd));
        }
        return items;
    }

    /**
     * 書類本体（type=5、CSV形式のXBRL代替データ、ZIP圧縮）をダウンロードし、財務諸表として組み立てる。
     * docTypeCode・periodEnd は fetchDocumentList() で取得済みの値をそのまま渡す
     * （EdinetMatchedDocument 経由、設計書 4.3.2 参照）。
     */
    public FinancialStatementData fetchStatementDocument(String docId, String docTypeCode, LocalDate periodEnd) {
        byte[] zipBytes;
        try {
            zipBytes = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/documents/{docId}")
                            .queryParam("type", "5")
                            .queryParam("Subscription-Key", apiProperties.getEdinet().getApiKey())
                            .build(docId))
                    .retrieve()
                    .body(byte[].class);
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new RateLimitException("EDINET document download rate limited: docId=" + docId, e);
        }
        if (zipBytes == null || zipBytes.length == 0) {
            return null;
        }
        Map<String, String> values = extractElementValues(zipBytes);
        return toFinancialStatementData(values, docTypeCode, periodEnd);
    }

    private Map<String, String> extractElementValues(byte[] zipBytes) {
        Map<String, String> values = new HashMap<>();
        Map<String, Integer> priorities = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".csv")) {
                    byte[] entryBytes = zis.readAllBytes();
                    parseCsvBytes(entryBytes, values, priorities);
                }
                zis.closeEntry();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse EDINET CSV document", e);
        }
        return values;
    }

    // EDINETのCSV代替データはUTF-16LE・タブ区切り。要素ID/コンテキストID/値の列を「値」で拾う。
    // 同一要素IDが複数コンテキスト（前期比較・個別ベース等）で出現するため、
    // 「当期（Current〜）」かつ「連結」の値を優先して採用する（無ければ当期の個別値、それも無ければ採用しない）。
    private void parseCsvBytes(byte[] csvBytes, Map<String, String> values, Map<String, Integer> priorities) {
        String content = new String(csvBytes, StandardCharsets.UTF_16LE);
        String[] lines = content.split("\r\n|\n");
        if (lines.length == 0) {
            return;
        }
        String[] header = lines[0].split("\t", -1);
        int elementIdIdx = indexOf(header, "要素ID");
        int contextIdIdx = indexOf(header, "コンテキストID");
        int valueIdx = indexOf(header, "値");
        if (elementIdIdx < 0 || valueIdx < 0) {
            return;
        }
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            String[] cols = lines[i].split("\t", -1);
            if (cols.length <= elementIdIdx || cols.length <= valueIdx) {
                continue;
            }
            String elementId = strip(cols[elementIdIdx]);
            String contextId = contextIdIdx >= 0 && cols.length > contextIdIdx ? strip(cols[contextIdIdx]) : "";
            String value = strip(cols[valueIdx]);
            if (elementId.isEmpty() || value.isEmpty() || !isCurrentPeriodContext(contextId)) {
                continue;
            }
            int priority = isConsolidatedContext(contextId) ? 2 : 1;
            Integer existing = priorities.get(elementId);
            if (existing == null || priority > existing) {
                values.put(elementId, value);
                priorities.put(elementId, priority);
            }
        }
    }

    private static int indexOf(String[] header, String columnName) {
        for (int i = 0; i < header.length; i++) {
            if (strip(header[i]).equals(columnName)) {
                return i;
            }
        }
        return -1;
    }

    private static String strip(String s) {
        String t = s.trim();
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            t = t.substring(1, t.length() - 1);
        }
        return t;
    }

    private static boolean isCurrentPeriodContext(String contextId) {
        return contextId.startsWith("CurrentYear") || contextId.startsWith("CurrentQuarter")
                || contextId.startsWith("Current");
    }

    private static boolean isConsolidatedContext(String contextId) {
        return !contextId.contains("NonConsolidated");
    }

    private FinancialStatementData toFinancialStatementData(Map<String, String> v, String docTypeCode, LocalDate periodEnd) {
        String fiscalQuarter = DOC_TYPE_YUHO.equals(docTypeCode) ? "Q4" : "Q2";
        Integer fiscalYear = periodEnd != null ? periodEnd.getYear() : null;

        BigDecimal revenue = decimal(v, "NetSales", "NetSalesSummaryOfBusinessResults");
        BigDecimal operatingIncome = decimal(v, "OperatingIncome");
        BigDecimal netIncome = decimal(v, "ProfitLossAttributableToOwnersOfParent", "ProfitLoss");
        BigDecimal eps = decimal(v, "BasicEarningsPerShare", "EarningsPerShare");
        BigDecimal operatingCashFlow = decimal(v, "NetCashProvidedByUsedInOperatingActivities");
        BigDecimal investingCashFlow = decimal(v, "NetCashProvidedByUsedInInvestingActivities");
        BigDecimal financingCf = decimal(v, "NetCashProvidedByUsedInFinancingActivities");
        BigDecimal totalAssets = decimal(v, "Assets");
        BigDecimal totalDebt = decimal(v, "Liabilities");
        BigDecimal totalEquity = decimal(v, "NetAssets");
        BigDecimal cashAndEquivalents = decimal(v, "CashAndDeposits", "CashAndCashEquivalents");
        BigDecimal grossProfit = decimal(v, "GrossProfit");
        BigDecimal sgAndA = decimal(v, "SellingGeneralAndAdministrativeExpenses");
        BigDecimal inventory = decimal(v, "Inventories", "MerchandiseAndFinishedGoods");
        BigDecimal accountsReceivable = decimal(v, "NotesAndAccountsReceivableTrade");
        BigDecimal interestExpense = decimal(v, "InterestExpense");
        BigDecimal dividendsPaid = decimal(v, "CashDividendsPaid", "DividendsPaidCF");
        BigDecimal freeCashFlow = (operatingCashFlow != null && investingCashFlow != null)
                ? operatingCashFlow.add(investingCashFlow)
                : null;
        // EDINETの財務諸表本表にはEBIT科目が無いため、営業利益で近似する
        BigDecimal ebit = operatingIncome;

        return new FinancialStatementData(
                fiscalYear, fiscalQuarter, periodEnd,
                revenue, operatingIncome, netIncome, eps, null,
                operatingCashFlow, investingCashFlow, freeCashFlow,
                totalAssets, totalDebt, cashAndEquivalents,
                grossProfit, sgAndA, ebit, totalEquity,
                inventory, accountsReceivable, financingCf,
                interestExpense, dividendsPaid);
    }

    private static BigDecimal decimal(Map<String, String> v, String... elementIdCandidates) {
        for (String key : elementIdCandidates) {
            String raw = v.get("jppfs_cor:" + key);
            if (raw == null) {
                raw = v.get(key);
            }
            if (raw != null && !raw.isBlank()) {
                try {
                    return new BigDecimal(raw.trim());
                } catch (NumberFormatException e) {
                    // "－" 等の非開示表記は数値変換できないため無視する
                }
            }
        }
        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EdinetDocumentListResponse(List<EdinetDocument> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EdinetDocument(
            String docID,
            String secCode,
            String docTypeCode,
            String periodEnd) {
    }
}
