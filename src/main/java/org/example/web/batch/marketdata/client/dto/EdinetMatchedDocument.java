package org.example.web.batch.marketdata.client.dto;

import java.time.LocalDate;

// companyId + docID の組。edinetDocumentListStep（Tasklet）の出力であり、
// jpFinancialStatementStep の EdinetMatchedDocumentItemReader が 1 件ずつ読み出す（設計書 3.2・4.3.2 参照）。
// docTypeCode / periodEnd は documents.json のレスポンスからそのまま引き継ぎ、
// JpFinancialStatementItemProcessor が fiscal_quarter（有報=Q4 / 半報=Q2）・fiscal_year の判定に使う。
public record EdinetMatchedDocument(
                Integer companyId,
                String docId,
                String docTypeCode,
                LocalDate periodEnd) {
}
