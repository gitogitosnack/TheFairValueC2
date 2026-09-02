package org.example.web.batch.marketdata.client;

import java.time.LocalDate;

import org.example.web.batch.marketdata.client.dto.StockQuoteData;
import org.example.web.batch.marketdata.config.YfinanceServiceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class YahooFinanceStockPriceProviderImplTest {

    private MockRestServiceServer server;

    private YahooFinanceStockPriceProviderImpl newProvider() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        return new YahooFinanceStockPriceProviderImpl(builder, new YfinanceServiceProperties());
    }

    @Test
    void fetchLatestQuote_成功時にyfinanceServiceの応答をStockQuoteDataへ変換すること() {
        YahooFinanceStockPriceProviderImpl provider = newProvider();
        server.expect(requestTo("http://localhost:8081/quotes/4452"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "symbol": "4452.T",
                          "date": "2026-08-28",
                          "open": 1000.0,
                          "high": 1050.0,
                          "low": 990.0,
                          "close": 1040.0,
                          "volume": 123456,
                          "marketCap": 999999999.0,
                          "sharesOutstanding": 987654321
                        }
                        """, MediaType.APPLICATION_JSON));

        StockQuoteData result = provider.fetchLatestQuote("4452");

        assertThat(result.date()).isEqualTo(LocalDate.of(2026, 8, 28));
        assertThat(result.openPrice()).isEqualByComparingTo("1000.0");
        assertThat(result.highPrice()).isEqualByComparingTo("1050.0");
        assertThat(result.lowPrice()).isEqualByComparingTo("990.0");
        assertThat(result.closePrice()).isEqualByComparingTo("1040.0");
        assertThat(result.volume()).isEqualTo(123456L);
        assertThat(result.sharesOutstanding()).isEqualTo(987654321L);
        server.verify();
    }

    @Test
    void fetchLatestQuote_日付未指定の場合は当日日付を補うこと() {
        YahooFinanceStockPriceProviderImpl provider = newProvider();
        server.expect(requestTo("http://localhost:8081/quotes/4452"))
                .andRespond(withSuccess("""
                        {
                          "symbol": "4452.T",
                          "close": 1040.0
                        }
                        """, MediaType.APPLICATION_JSON));

        StockQuoteData result = provider.fetchLatestQuote("4452");

        assertThat(result.date()).isEqualTo(LocalDate.now());
        server.verify();
    }

    @Test
    void fetchLatestQuote_404の場合はnullを返すこと() {
        YahooFinanceStockPriceProviderImpl provider = newProvider();
        server.expect(requestTo("http://localhost:8081/quotes/9999"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        StockQuoteData result = provider.fetchLatestQuote("9999");

        assertThat(result).isNull();
        server.verify();
    }

    @Test
    void fetchLatestQuote_429の場合はRateLimitExceptionをthrowすること() {
        YahooFinanceStockPriceProviderImpl provider = newProvider();
        server.expect(requestTo("http://localhost:8081/quotes/4452"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> provider.fetchLatestQuote("4452"))
                .isInstanceOf(RateLimitException.class);
        server.verify();
    }

    @Test
    void isHealthy_疎通できるときtrueを返すこと() {
        YahooFinanceStockPriceProviderImpl provider = newProvider();
        server.expect(requestTo("http://localhost:8081/health"))
                .andRespond(withSuccess());

        assertThat(provider.isHealthy()).isTrue();
        server.verify();
    }

    @Test
    void isHealthy_疎通できないときfalseを返すこと() {
        YahooFinanceStockPriceProviderImpl provider = newProvider();
        server.expect(requestTo("http://localhost:8081/health"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThat(provider.isHealthy()).isFalse();
        server.verify();
    }
}
