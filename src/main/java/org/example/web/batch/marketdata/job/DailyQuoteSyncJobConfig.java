package org.example.web.batch.marketdata.job;

// dailyQuoteSyncJob / dailyQuoteSyncStep（chunk 指向、単一 Step）の Spring Batch Bean 定義。
// CompanyItemReader → DailyQuoteItemProcessor → DailyQuoteItemWriter の chunk 構成。
// skip/retry の faultTolerant 設定（RateLimitException のリトライ等）もここに持つ（設計書 4.2・7 章参照）。
