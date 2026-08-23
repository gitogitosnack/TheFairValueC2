package org.example.web.batch.marketdata.scheduler;

// 内蔵 @Scheduled エントリポイント。cron 式は環境変数（MarketDataScheduleProperties）で設定する。
// MARKETDATA_SCHEDULER_ENABLED=true のときのみ Bean 登録する（@ConditionalOnProperty）。
// 起動前に BatchExecutionLockService でロック取得を試みる（設計書 3.1・3.2・3.4 参照）。
