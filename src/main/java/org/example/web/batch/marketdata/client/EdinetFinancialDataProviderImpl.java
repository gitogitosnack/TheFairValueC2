package org.example.web.batch.marketdata.client;

// FinancialDataProvider の日本株向け実装。EDINET API v2（documents.json / documents/{docID}）を
// 呼び出す（設計書 5.1 参照）。実際の書類一覧取得・絞り込みは EdinetDocumentListTasklet が担う。
