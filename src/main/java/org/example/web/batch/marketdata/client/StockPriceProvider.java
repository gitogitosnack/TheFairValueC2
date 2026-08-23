package org.example.web.batch.marketdata.client;

// 株価取得の抽象化インタフェース。銘柄の country_id / currency_id から実装（Bean）を切り替える
// （YahooFinanceStockPriceProviderImpl / FmpStockPriceProviderImpl、設計書 4.1 参照）。
