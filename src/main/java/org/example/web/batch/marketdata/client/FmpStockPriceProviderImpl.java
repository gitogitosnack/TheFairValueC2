package org.example.web.batch.marketdata.client;

import java.math.BigDecimal;
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
// GET /api/v3/quote/{symbol} で現在値・出来高・時価総額・発行済株式数等を取得する（設計書 5.2 参照）。
@Component("fmpStockPriceProvider")
public class FmpStockPriceProviderImpl implements StockPriceProvider {

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
                    .uri(uriBuilder -> uriBuilder.path("/quote/{symbol}")
                            .queryParam("apikey", apiProperties.getFmp().getApiKey())
                            .build(code))
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
        LocalDate date = quote.timestamp() != null
                ? Instant.ofEpochSecond(quote.timestamp()).atZone(ZoneId.systemDefault()).toLocalDate()
                : LocalDate.now();
        return new StockQuoteData(
                date,
                quote.open(),
                quote.dayHigh(),
                quote.dayLow(),
                quote.price(),
                quote.volume(),
                quote.marketCap(),
                quote.sharesOutstanding());
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
