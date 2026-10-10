package org.example.web.batch.marketdata.client;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.example.web.batch.marketdata.client.dto.StockQuoteData;
import org.example.web.batch.marketdata.config.MarketDataApiProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// StockPriceProvider の米国株向け実装。Financial Modeling Prep（FMP）の
// GET /stable/quote?symbol={symbol} で現在値・出来高・時価総額等を取得する（設計書 5.2 参照）。
// 旧 /api/v3 は 2025-08-31 以前からの契約者専用となり新規キーでは使えないため stable 版を使う。
// stable 版の quote は sharesOutstanding を返さないので、発行済株式数は marketCap / price で求める。
@Component("fmpStockPriceProvider")
public class FmpStockPriceProviderImpl implements StockPriceProvider {

    private static final ZoneId US_MARKET_ZONE = ZoneId.of("America/New_York");

    private final RestClient restClient;
    private final MarketDataApiProperties apiProperties;

    public FmpStockPriceProviderImpl(
            RestClient.Builder marketDataRestClientBuilder,
            MarketDataApiProperties apiProperties) {
        this.apiProperties = apiProperties;
        this.restClient = marketDataRestClientBuilder.clone()
                .baseUrl(apiProperties.getFmp().getBaseUrl())
                .build();
    }

    @Override
    public StockQuoteData fetchLatestQuote(String code) {
        FmpQuoteResponse[] response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/quote")
                            .queryParam("symbol", code)
                            .queryParam("apikey", apiProperties.getFmp().getApiKey())
                            .build())
                    .retrieve()
                    .body(FmpQuoteResponse[].class);
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new RateLimitException("FMP quote API rate limited: code=" + code, e);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
        if (response == null || response.length == 0) {
            return null;
        }
        FmpQuoteResponse quote = response[0];
        // 取引日は米国市場のタイムゾーンで決める（JVM 既定の JST だと終値時刻が翌日扱いになる）
        LocalDate date = quote.timestamp() != null
                ? Instant.ofEpochSecond(quote.timestamp()).atZone(US_MARKET_ZONE).toLocalDate()
                : LocalDate.now(US_MARKET_ZONE);
        return new StockQuoteData(
                date,
                quote.open(),
                quote.dayHigh(),
                quote.dayLow(),
                quote.price(),
                quote.volume(),
                quote.marketCap(),
                sharesOutstanding(quote));
    }

    private static Long sharesOutstanding(FmpQuoteResponse quote) {
        if (quote.sharesOutstanding() != null) {
            return quote.sharesOutstanding();
        }
        if (quote.marketCap() == null || quote.price() == null || quote.price().signum() == 0) {
            return null;
        }
        return quote.marketCap().divide(quote.price(), 0, RoundingMode.HALF_UP).longValue();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FmpQuoteResponse(
            String symbol,
            BigDecimal price,
            BigDecimal open,
            BigDecimal dayHigh,
            BigDecimal dayLow,
            Long volume,
            BigDecimal marketCap,
            Long sharesOutstanding,
            Long timestamp) {
    }
}
