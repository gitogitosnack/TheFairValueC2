package org.example.web.batch.marketdata.job;

// financialStatementSyncJob の Spring Batch Bean 定義。
// usFinancialStatementStep → edinetDocumentListStep → jpFinancialStatementStep の順に
// 単一 Job 内で直列実行する（並行実行はしない、設計書 4.3 参照）。
