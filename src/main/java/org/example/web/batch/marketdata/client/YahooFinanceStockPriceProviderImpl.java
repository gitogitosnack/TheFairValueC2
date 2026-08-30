package org.example.web.batch.marketdata.client;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.example.web.batch.marketdata.client.dto.StockQuoteData;
import org.example.web.batch.marketdata.config.YfinanceServiceProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// StockPriceProvider の日本株向け実装。Yahoo Finance を直接叩くのではなく、
// python-services/yfinance-service を RestClientConfig の HTTP クライアントで呼び出すだけの
// シンプルなクライアントになる。ベース URL は YfinanceServiceProperties で設定する（設計書 3.3 参照）。
@Component("yahooFinanceStockPriceProvider")
public class YahooFinanceStockPriceProviderImpl implements StockPriceProvider {

    private final RestClient restClient;

    public YahooFinanceStockPriceProviderImpl(
            RestClient.Builder marketDataRestClientBuilder,
            YfinanceServiceProperties yfinanceServiceProperties) {
        this.restClient = marketDataRestClientBuilder.clone()
                .baseUrl(yfinanceServiceProperties.getBaseUrl())
                .build();
    }

    @Override
    public StockQuoteData fetchLatestQuote(String code) {
        YfinanceQuoteResponse response;
        try {
            response = restClient.get()
                    .uri("/quotes/{code}", code)
                    .retrieve()
                    .body(YfinanceQuoteResponse.class);
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new RateLimitException("yfinance-service rate limited: code=" + code, e);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
        if (response == null) {
            return null;
        }
        LocalDate date = response.date() != null ? LocalDate.parse(response.date()) : LocalDate.now();
        return new StockQuoteData(
                date,
                response.open(),
                response.high(),
                response.low(),
                response.close(),
                response.volume(),
                response.marketCap(),
                response.sharesOutstanding());
    }

    /**
     * yfinance-service が起動済み・応答可能かを確認する。バッチ冒頭のヘルスチェック用（設計書 5.3・7 章参照）。
     */
    public boolean isHealthy() {
        try {
            restClient.get().uri("/health").retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record YfinanceQuoteResponse(
            String symbol,
            String date,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            Long volume,
            BigDecimal marketCap,
            Long sharesOutstanding) {
    }
}
