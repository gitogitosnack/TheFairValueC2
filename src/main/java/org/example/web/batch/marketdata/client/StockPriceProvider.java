package org.example.web.batch.marketdata.client;

import org.example.web.batch.marketdata.client.dto.StockQuoteData;

// 株価取得の抽象化インタフェース。銘柄の country_id / currency_id から実装（Bean）を切り替える
// （YahooFinanceStockPriceProviderImpl / FmpStockPriceProviderImpl、設計書 4.1 参照）。
public interface StockPriceProvider {

    /**
     * 指定銘柄コードの当日株価（始値・高値・安値・終値・出来高・時価総額・発行済株式数）を取得する。
     * 取得対象が存在しない場合は null を返す。429（レート制限）の場合は {@link RateLimitException} を throw する。
     */
    StockQuoteData fetchLatestQuote(String code);
}
