package org.example.web.batch.marketdata.client;

import org.example.web.batch.marketdata.client.dto.StockQuoteData;
import org.example.web.batch.marketdata.config.MarketDataApiProperties;
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

class FmpStockPriceProviderImplTest {

    private MockRestServiceServer server;

    private FmpStockPriceProviderImpl newProvider() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        MarketDataApiProperties properties = new MarketDataApiProperties();
        properties.getFmp().setApiKey("test-api-key");
        return new FmpStockPriceProviderImpl(builder, properties);
    }

    @Test
    void fetchLatestQuote_成功時にFMPの応答をStockQuoteDataへ変換すること() {
        FmpStockPriceProviderImpl provider = newProvider();
        server.expect(requestTo("https://financialmodelingprep.com/api/v3/quote/AAPL?apikey=test-api-key"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "symbol": "AAPL",
                            "price": 190.5,
                            "open": 188.0,
                            "dayHigh": 191.0,
                            "dayLow": 187.5,
                            "volume": 50000000,
                            "marketCap": 3000000000000,
                            "sharesOutstanding": 15700000000,
                            "timestamp": 1735358400
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        StockQuoteData result = provider.fetchLatestQuote("AAPL");

        assertThat(result.closePrice()).isEqualByComparingTo("190.5");
        assertThat(result.openPrice()).isEqualByComparingTo("188.0");
        assertThat(result.highPrice()).isEqualByComparingTo("191.0");
        assertThat(result.lowPrice()).isEqualByComparingTo("187.5");
        assertThat(result.volume()).isEqualTo(50000000L);
        assertThat(result.marketCap()).isEqualByComparingTo("3000000000000");
        assertThat(result.sharesOutstanding()).isEqualTo(15700000000L);
        server.verify();
    }

    @Test
    void fetchLatestQuote_空配列の場合はnullを返すこと() {
        FmpStockPriceProviderImpl provider = newProvider();
        server.expect(requestTo("https://financialmodelingprep.com/api/v3/quote/UNKNOWN?apikey=test-api-key"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        StockQuoteData result = provider.fetchLatestQuote("UNKNOWN");

        assertThat(result).isNull();
        server.verify();
    }

    @Test
    void fetchLatestQuote_404の場合はnullを返すこと() {
        FmpStockPriceProviderImpl provider = newProvider();
        server.expect(requestTo("https://financialmodelingprep.com/api/v3/quote/UNKNOWN?apikey=test-api-key"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        StockQuoteData result = provider.fetchLatestQuote("UNKNOWN");

        assertThat(result).isNull();
        server.verify();
    }

    @Test
    void fetchLatestQuote_429の場合はRateLimitExceptionをthrowすること() {
        FmpStockPriceProviderImpl provider = newProvider();
        server.expect(requestTo("https://financialmodelingprep.com/api/v3/quote/AAPL?apikey=test-api-key"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> provider.fetchLatestQuote("AAPL"))
                .isInstanceOf(RateLimitException.class);
        server.verify();
    }
}
