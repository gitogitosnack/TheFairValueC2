package org.example.web.batch.marketdata.processor;

import org.example.web.batch.marketdata.client.EdinetFinancialDataProviderImpl;
import org.example.web.batch.marketdata.client.RateLimitException;
import org.example.web.batch.marketdata.client.dto.EdinetMatchedDocument;
import org.example.web.batch.marketdata.client.dto.FinancialStatementData;
import org.example.web.entity.FinancialStatementEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

// ItemProcessor<EdinetMatchedDocument, FinancialStatementEntity>。documents/{docID}（type=5, CSV形式の
// XBRL代替データ）をダウンロードし、売上高・営業利益・純利益・EPS・BPS 等の勘定科目タクソノミ要素を
// パースして FinancialStatementEntity を組み立てる（設計書 4.3.2 参照）。
@Component
public class JpFinancialStatementItemProcessor implements ItemProcessor<EdinetMatchedDocument, FinancialStatementEntity> {

    private static final Logger log = LoggerFactory.getLogger(JpFinancialStatementItemProcessor.class);

    private final EdinetFinancialDataProviderImpl edinetFinancialDataProvider;

    public JpFinancialStatementItemProcessor(EdinetFinancialDataProviderImpl edinetFinancialDataProvider) {
        this.edinetFinancialDataProvider = edinetFinancialDataProvider;
    }

    @Override
    public FinancialStatementEntity process(EdinetMatchedDocument matchedDocument) {
        FinancialStatementData apiData;
        try {
            apiData = edinetFinancialDataProvider.fetchStatementDocument(
                    matchedDocument.docId(), matchedDocument.docTypeCode(), matchedDocument.periodEnd());
        } catch (RateLimitException e) {
            throw e;
        } catch (Exception e) {
            log.warn("EDINET 書類の取得・パースに失敗しました。次回バッチで再取得します: companyId={}, docId={}",
                    matchedDocument.companyId(), matchedDocument.docId(), e);
            return null;
        }
        if (apiData == null || apiData.fiscalYear() == null || apiData.endDate() == null) {
            log.warn("EDINET 書類から財務データを組み立てられませんでした: companyId={}, docId={}",
                    matchedDocument.companyId(), matchedDocument.docId());
            return null;
        }

        return FinancialStatementEntityMapper.toEntity(matchedDocument.companyId(), apiData);
    }
}
