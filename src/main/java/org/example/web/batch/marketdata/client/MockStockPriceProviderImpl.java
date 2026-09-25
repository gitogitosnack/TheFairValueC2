package org.example.web.batch.marketdata.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.example.web.batch.marketdata.client.dto.StockQuoteData;
import org.springframework.stereotype.Component;

// StockPriceProvider のモック実装。外部API（yfinance-service / FMP）を一切呼び出さず、
// MockSampleCompanies に登録した日米比 2 社ずつ・計 6 社分の当日株価サンプルを返す。
// marketdata.api.mock-enabled=true のとき DailyQuoteItemProcessor がこの Bean（"mockStockPriceProvider"）に
// 全銘柄をルーティングする（国コードによる振り分けを行わないため、サンプル未登録のコードは null を返す）。
@Component("mockStockPriceProvider")
public class MockStockPriceProviderImpl implements StockPriceProvider {

    private record SampleQuote(
            BigDecimal openPrice, BigDecimal highPrice, BigDecimal lowPrice, BigDecimal closePrice,
            long volume, BigDecimal marketCap, long sharesOutstanding) {
    }

    private static final Map<String, SampleQuote> SAMPLE_QUOTES = Map.of(
            // トヨタ自動車（JPY）
            "7203", new SampleQuote(
                    new BigDecimal("2780"), new BigDecimal("2815"), new BigDecimal("2765"), new BigDecimal("2802"),
                    15_200_000L, new BigDecimal("45200000000000"), 16_100_000_000L),
            // ソニーグループ（JPY）
            "6758", new SampleQuote(
                    new BigDecimal("3460"), new BigDecimal("3505"), new BigDecimal("3442"), new BigDecimal("3488"),
                    8_100_000L, new BigDecimal("17300000000000"), 4_960_000_000L),
            // Apple（USD）
            "AAPL", new SampleQuote(
                    new BigDecimal("193.20"), new BigDecimal("196.80"), new BigDecimal("192.55"), new BigDecimal("195.40"),
                    54_800_000L, new BigDecimal("3010000000000"), 15_400_000_000L),
            // Microsoft（USD）
            "MSFT", new SampleQuote(
                    new BigDecimal("417.10"), new BigDecimal("422.90"), new BigDecimal("415.30"), new BigDecimal("420.15"),
                    19_600_000L, new BigDecimal("3120000000000"), 7_430_000_000L),
            // BDOユニバンク（PHP）
            "BDO", new SampleQuote(
                    new BigDecimal("138.50"), new BigDecimal("141.00"), new BigDecimal("137.80"), new BigDecimal("140.20"),
                    2_950_000L, new BigDecimal("603000000000"), 4_300_000_000L),
            // フィリピン諸島銀行（PHP）
            "BPI", new SampleQuote(
                    new BigDecimal("118.00"), new BigDecimal("120.50"), new BigDecimal("117.20"), new BigDecimal("119.60"),
                    1_870_000L, new BigDecimal("346000000000"), 2_890_000_000L));

    @Override
    public StockQuoteData fetchLatestQuote(String code) {
        SampleQuote sample = SAMPLE_QUOTES.get(code);
        if (sample == null) {
            return null;
        }
        return new StockQuoteData(
                LocalDate.now(),
                sample.openPrice(),
                sample.highPrice(),
                sample.lowPrice(),
                sample.closePrice(),
                sample.volume(),
                sample.marketCap(),
                sample.sharesOutstanding());
    }
}
