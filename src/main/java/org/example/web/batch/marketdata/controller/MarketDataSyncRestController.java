package org.example.web.batch.marketdata.controller;

// 手動実行用 REST エンドポイント。
// POST /rest_market_data_sync/quote            … 全銘柄の日次株価を即時同期
// POST /rest_market_data_sync/financial-statement … 全銘柄の財務諸表を即時同期（米国株→日本株の順で逐次実行）
// ロック取得に失敗した場合は 409 相当のレスポンスを返す（設計書 3.4・4.4 参照）。
