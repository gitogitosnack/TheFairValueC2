package org.example.web.batch.marketdata.client;

// StockPriceProvider の日本株向け実装。Yahoo Finance を直接叩くのではなく、
// python-services/yfinance-service を RestClientConfig の HTTP クライアントで呼び出すだけの
// シンプルなクライアントになる。ベース URL は YfinanceServiceProperties で設定する（設計書 3.3 参照）。
