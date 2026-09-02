package org.example.web.batch.marketdata.client;

import org.example.web.batch.marketdata.client.dto.EdinetDocumentListItem;
import org.example.web.batch.marketdata.client.dto.FinancialStatementData;
import org.example.web.batch.marketdata.config.MarketDataApiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EdinetFinancialDataProviderImplTest {

    private static final String BASE = "https://api.edinet-fsa.go.jp/api/v2";

    private MockRestServiceServer server;

    private EdinetFinancialDataProviderImpl newProvider() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        MarketDataApiProperties properties = new MarketDataApiProperties();
        properties.getEdinet().setApiKey("test-edinet-key");
        return new EdinetFinancialDataProviderImpl(builder, properties);
    }

    private static byte[] zipOf(String entryName, String csvContent) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                zos.putNextEntry(new ZipEntry(entryName));
                zos.write(csvContent.getBytes(StandardCharsets.UTF_16LE));
                zos.closeEntry();
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void fetchLatestStatement_EDINETは単発コード取得に対応しないため例外を投げること() {
        EdinetFinancialDataProviderImpl provider = newProvider();

        assertThatThrownBy(() -> provider.fetchLatestStatement("4452"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void fetchDocumentList_有報と半報のみに絞り込んで返すこと() {
        EdinetFinancialDataProviderImpl provider = newProvider();
        server.expect(requestTo(BASE + "/documents.json?date=2026-08-28&type=2&Subscription-Key=test-edinet-key"))
                .andRespond(withSuccess("""
                        {
                          "results": [
                            { "docID": "S100YUHO", "secCode": "43050", "docTypeCode": "120", "periodEnd": "2026-06-30" },
                            { "docID": "S100HANKI", "secCode": "72030", "docTypeCode": "160", "periodEnd": "2026-06-30" },
                            { "docID": "S100OTHER", "secCode": "99990", "docTypeCode": "030", "periodEnd": "2026-06-30" },
                            { "docID": "S100NOTYPE", "secCode": "88880" }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<EdinetDocumentListItem> result = provider.fetchDocumentList(LocalDate.of(2026, 8, 28));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(EdinetDocumentListItem::docId)
                .containsExactlyInAnyOrder("S100YUHO", "S100HANKI");
        EdinetDocumentListItem yuho = result.stream().filter(i -> i.docId().equals("S100YUHO")).findFirst().orElseThrow();
        assertThat(yuho.secCode()).isEqualTo("43050");
        assertThat(yuho.docTypeCode()).isEqualTo("120");
        assertThat(yuho.periodEnd()).isEqualTo(LocalDate.of(2026, 6, 30));
        server.verify();
    }

    @Test
    void fetchDocumentList_429の場合はRateLimitExceptionをthrowすること() {
        EdinetFinancialDataProviderImpl provider = newProvider();
        server.expect(requestTo(BASE + "/documents.json?date=2026-08-28&type=2&Subscription-Key=test-edinet-key"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> provider.fetchDocumentList(LocalDate.of(2026, 8, 28)))
                .isInstanceOf(RateLimitException.class);
        server.verify();
    }

    @Test
    void fetchStatementDocument_CSV代替データをパースしFinancialStatementDataを組み立てること() {
        EdinetFinancialDataProviderImpl provider = newProvider();
        String csv = "要素ID\tコンテキストID\t値\n"
                // 個別値が先に来ても、後続の連結値で上書きされること
                + "jppfs_cor:NetSales\tCurrentYearDuration_NonConsolidatedMember\t900000\n"
                + "jppfs_cor:NetSales\tCurrentYearDuration\t1000000\n"
                + "jppfs_cor:OperatingIncome\tCurrentYearDuration\t200000\n"
                + "jppfs_cor:ProfitLossAttributableToOwnersOfParent\tCurrentYearDuration\t150000\n"
                + "jppfs_cor:BasicEarningsPerShare\tCurrentYearDuration\t15.5\n"
                + "jppfs_cor:Assets\tCurrentYearInstant\t5000000\n"
                + "jppfs_cor:Liabilities\tCurrentYearInstant\t3000000\n"
                + "jppfs_cor:NetAssets\tCurrentYearInstant\t2000000\n"
                + "jppfs_cor:CashAndDeposits\tCurrentYearInstant\t800000\n"
                + "jppfs_cor:NetCashProvidedByUsedInOperatingActivities\tCurrentYearDuration\t250000\n"
                + "jppfs_cor:NetCashProvidedByUsedInInvestingActivities\tCurrentYearDuration\t-80000\n"
                // 前期比較値は無視されること（採用されれば売上高が壊れてテストが失敗する）
                + "jppfs_cor:NetSales\tPrior1YearDuration\t999999999\n";
        byte[] zip = zipOf("XBRL_TO_CSV/jpcrp030000-asr-001_sample.csv", csv);
        server.expect(requestTo(BASE + "/documents/S100YUHO?type=5&Subscription-Key=test-edinet-key"))
                .andRespond(withSuccess(zip, MediaType.APPLICATION_OCTET_STREAM));

        FinancialStatementData result = provider.fetchStatementDocument("S100YUHO", "120", LocalDate.of(2026, 6, 30));

        assertThat(result.fiscalYear()).isEqualTo(2026);
        assertThat(result.fiscalQuarter()).isEqualTo("Q4");
        assertThat(result.endDate()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(result.revenue()).isEqualByComparingTo("1000000");
        assertThat(result.operatingIncome()).isEqualByComparingTo("200000");
        assertThat(result.netIncome()).isEqualByComparingTo("150000");
        assertThat(result.eps()).isEqualByComparingTo("15.5");
        assertThat(result.totalAssets()).isEqualByComparingTo("5000000");
        assertThat(result.totalDebt()).isEqualByComparingTo("3000000");
        assertThat(result.totalEquity()).isEqualByComparingTo("2000000");
        assertThat(result.cashAndEquivalents()).isEqualByComparingTo("800000");
        assertThat(result.operatingCashFlow()).isEqualByComparingTo("250000");
        assertThat(result.investingCashFlow()).isEqualByComparingTo("-80000");
        // freeCashFlow = operatingCashFlow + investingCashFlow
        assertThat(result.freeCashFlow()).isEqualByComparingTo("170000");
        // ebit は営業利益で近似する
        assertThat(result.ebit()).isEqualByComparingTo("200000");
        assertThat(result.bps()).isNull();
        server.verify();
    }

    @Test
    void fetchStatementDocument_半期報告書はfiscalQuarterがQ2になること() {
        EdinetFinancialDataProviderImpl provider = newProvider();
        String csv = "要素ID\tコンテキストID\t値\n"
                + "jppfs_cor:NetSales\tCurrentYearDuration\t500000\n";
        byte[] zip = zipOf("XBRL_TO_CSV/jpcrp040300-q2r-001_sample.csv", csv);
        server.expect(requestTo(BASE + "/documents/S100HANKI?type=5&Subscription-Key=test-edinet-key"))
                .andRespond(withSuccess(zip, MediaType.APPLICATION_OCTET_STREAM));

        FinancialStatementData result = provider.fetchStatementDocument("S100HANKI", "160", LocalDate.of(2026, 9, 30));

        assertThat(result.fiscalQuarter()).isEqualTo("Q2");
        assertThat(result.fiscalYear()).isEqualTo(2026);
        assertThat(result.revenue()).isEqualByComparingTo("500000");
        server.verify();
    }

    @Test
    void fetchStatementDocument_429の場合はRateLimitExceptionをthrowすること() {
        EdinetFinancialDataProviderImpl provider = newProvider();
        server.expect(requestTo(BASE + "/documents/S100YUHO?type=5&Subscription-Key=test-edinet-key"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> provider.fetchStatementDocument("S100YUHO", "120", LocalDate.of(2026, 6, 30)))
                .isInstanceOf(RateLimitException.class);
        server.verify();
    }
}
