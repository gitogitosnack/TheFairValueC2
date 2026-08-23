package org.example.web.batch.marketdata.processor;

// ItemProcessor<EdinetMatchedDocument, FinancialStatementEntity>。documents/{docID}（type=5, CSV形式の
// XBRL代替データ）をダウンロードし、売上高・営業利益・純利益・EPS・BPS 等の勘定科目タクソノミ要素を
// パースして FinancialStatementEntity を組み立てる（設計書 4.3.2 参照）。
