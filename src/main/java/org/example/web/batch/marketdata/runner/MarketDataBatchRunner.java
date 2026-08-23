package org.example.web.batch.marketdata.runner;

// 外部スケジューラ用エントリポイント（CommandLineRunner）。
// 将来はバッチ専用メインクラス org.example.MarketDataBatchApplication から起動され、
// --job=dailyQuote / --job=financialStatement 引数でどちらの JobLauncher.run(job, jobParameters) を
// 呼ぶかを判定する。起動前に BatchExecutionLockService でロック取得を試みる（設計書 3.1・3.2・3.4 参照）。
