package org.example.web.batch.marketdata.processor;

// ItemProcessor<CompanyEntity, DailyQuoteEntity>。StockPriceProvider を呼び出して当日の
// 始値・高値・安値・終値・出来高・時価総額・発行済株式数を取得し変換する。
// 取得失敗（404・タイムアウト等）はログを残し null を返してそのアイテムを chunk から除外する。
// 429（レート制限）は RateLimitException を throw し Step 側の faultTolerant().retry() に委ねる（設計書 4.2 参照）。
