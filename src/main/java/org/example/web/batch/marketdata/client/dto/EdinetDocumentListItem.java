package org.example.web.batch.marketdata.client.dto;

import java.time.LocalDate;

// EDINET の書類一覧 API（documents.json）1 件分から、財務諸表取り込みに必要な項目だけを抜き出したもの。
// EdinetFinancialDataProviderImpl.fetchDocumentList() の戻り値要素であり、
// EdinetDocumentListTasklet が secCode を companies.code と突合して EdinetMatchedDocument へ絞り込む
// （設計書 4.3.2 参照）。
public record EdinetDocumentListItem(
        String docId,
        String secCode,
        String docTypeCode,
        LocalDate periodEnd) {
}
