package org.example.web.batch.marketdata.reader;

import java.util.List;

import org.example.web.batch.marketdata.client.dto.EdinetMatchedDocument;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// ItemReader<EdinetMatchedDocument>。edinetDocumentListStep が ExecutionContext へ格納した
// 「絞り込み済み書類リスト」を 1 件ずつ返す。jpFinancialStatementStep の Reader（設計書 4.3.2 参照）。
//
// edinetDocumentListStep（Tasklet）が Step の ExecutionContext に格納したリストは、
// FinancialStatementSyncJobConfig で登録する ExecutionContextPromotionListener によって
// Job の ExecutionContext（jobExecutionContext）へ昇格される。
@Component
@StepScope
public class EdinetMatchedDocumentItemReader extends ListItemReader<EdinetMatchedDocument> {

    public static final String EXECUTION_CONTEXT_KEY = "edinetMatchedDocuments";

    public EdinetMatchedDocumentItemReader(
            @Value("#{jobExecutionContext['" + EXECUTION_CONTEXT_KEY + "']}") List<EdinetMatchedDocument> matchedDocuments) {
        super(matchedDocuments != null ? matchedDocuments : List.of());
    }
}
