package org.example.web.batch.marketdata.client.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

// StockPriceProvider が返す、当日の株価・出来高・時価総額・発行済株式数のまとまり。
// DailyQuoteItemProcessor がこれを DailyQuoteEntity へ変換する（設計書 4.2 参照）。
public record StockQuoteData(
        LocalDate date,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice,
        Long volume,
        BigDecimal marketCap,
        Long sharesOutstanding) {
}
