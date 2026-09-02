package org.example.web.batch.marketdata.client;

import org.example.web.batch.marketdata.client.dto.FinancialStatementData;
import org.example.web.batch.marketdata.config.MarketDataApiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FmpFinancialDataProviderImplTest {

    private static final String BASE = "https://financialmodelingprep.com/api/v3";
    private static final String INCOME_URI = BASE + "/income-statement/AAPL?period=quarter&limit=1&apikey=test-api-key";
    private static final String BALANCE_URI = BASE + "/balance-sheet-statement/AAPL?period=quarter&limit=1&apikey=test-api-key";
    private static final String CASH_FLOW_URI = BASE + "/cash-flow-statement/AAPL?period=quarter&limit=1&apikey=test-api-key";

    private MockRestServiceServer server;

    private FmpFinancialDataProviderImpl newProvider() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        MarketDataApiProperties properties = new MarketDataApiProperties();
        properties.getFmp().setApiKey("test-api-key");
        return new FmpFinancialDataProviderImpl(builder, properties);
    }

    private static final String INCOME_BODY = """
            [
              {
                "date": "2026-06-30",
                "period": "Q2",
                "calendarYear": "2026",
                "revenue": 100000,
                "grossProfit": 40000,
                "operatingIncome": 20000,
                "netIncome": 15000,
                "eps": 1.5,
                "weightedAverageShsOut": 10000,
                "sellingGeneralAndAdministrativeExpenses": 5000,
                "interestExpense": 500
              }
            ]
            """;

    private static final String BALANCE_BODY = """
            [
              {
                "totalAssets": 200000,
                "totalLiabilities": 120000,
                "totalStockholdersEquity": 80000,
                "cashAndCashEquivalents": 30000,
                "inventory": 10000,
                "netReceivables": 8000
              }
            ]
            """;

    private static final String CASH_FLOW_BODY = """
            [
              {
                "operatingCashFlow": 25000,
                "netCashUsedForInvestingActivites": -8000,
                "netCashUsedProvidedByFinancingActivities": -3000,
                "freeCashFlow": 17000,
                "dividendsPaid": -2000
              }
            ]
            """;

    @Test
    void fetchLatestStatement_3つのエンドポイントの結果を1つのFinancialStatementDataへ統合すること() {
        FmpFinancialDataProviderImpl provider = newProvider();
        server.expect(requestTo(INCOME_URI)).andRespond(withSuccess(INCOME_BODY, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BALANCE_URI)).andRespond(withSuccess(BALANCE_BODY, MediaType.APPLICATION_JSON));
        server.expect(requestTo(CASH_FLOW_URI)).andRespond(withSuccess(CASH_FLOW_BODY, MediaType.APPLICATION_JSON));

        FinancialStatementData result = provider.fetchLatestStatement("AAPL");

        assertThat(result.fiscalYear()).isEqualTo(2026);
        assertThat(result.fiscalQuarter()).isEqualTo("Q2");
        assertThat(result.endDate()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(result.revenue()).isEqualByComparingTo("100000");
        assertThat(result.operatingIncome()).isEqualByComparingTo("20000");
        assertThat(result.netIncome()).isEqualByComparingTo("15000");
        assertThat(result.eps()).isEqualByComparingTo("1.5");
        // bps = totalStockholdersEquity(80000) / weightedAverageShsOut(10000)
        assertThat(result.bps()).isEqualByComparingTo("8.0000");
        assertThat(result.operatingCashFlow()).isEqualByComparingTo("25000");
        assertThat(result.investingCashFlow()).isEqualByComparingTo("-8000");
        assertThat(result.freeCashFlow()).isEqualByComparingTo("17000");
        assertThat(result.totalAssets()).isEqualByComparingTo("200000");
        assertThat(result.totalDebt()).isEqualByComparingTo("120000");
        assertThat(result.cashAndEquivalents()).isEqualByComparingTo("30000");
        assertThat(result.grossProfit()).isEqualByComparingTo("40000");
        assertThat(result.sgAndA()).isEqualByComparingTo("5000");
        assertThat(result.ebit()).isEqualByComparingTo("20000");
        assertThat(result.totalEquity()).isEqualByComparingTo("80000");
        assertThat(result.inventory()).isEqualByComparingTo("10000");
        assertThat(result.accountsReceivable()).isEqualByComparingTo("8000");
        assertThat(result.financingCf()).isEqualByComparingTo("-3000");
        assertThat(result.interestExpense()).isEqualByComparingTo("500");
        assertThat(result.dividendsPaid()).isEqualByComparingTo("-2000");
        server.verify();
    }

    @Test
    void fetchLatestStatement_損益計算書が空配列のときnullを返しBS及びCFは呼び出さないこと() {
        FmpFinancialDataProviderImpl provider = newProvider();
        server.expect(requestTo(INCOME_URI)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        FinancialStatementData result = provider.fetchLatestStatement("AAPL");

        assertThat(result).isNull();
        server.verify();
    }

    @Test
    void fetchLatestStatement_BSとCFが取得できなくても損益計算書の値だけで結果を返すこと() {
        FmpFinancialDataProviderImpl provider = newProvider();
        server.expect(requestTo(INCOME_URI)).andRespond(withSuccess(INCOME_BODY, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BALANCE_URI)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo(CASH_FLOW_URI)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        FinancialStatementData result = provider.fetchLatestStatement("AAPL");

        assertThat(result.revenue()).isEqualByComparingTo("100000");
        assertThat(result.bps()).isNull();
        assertThat(result.totalAssets()).isNull();
        assertThat(result.operatingCashFlow()).isNull();
        server.verify();
    }

    @Test
    void fetchLatestStatement_損益計算書取得時に429が返るとRateLimitExceptionをthrowすること() {
        FmpFinancialDataProviderImpl provider = newProvider();
        server.expect(requestTo(INCOME_URI)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> provider.fetchLatestStatement("AAPL"))
                .isInstanceOf(RateLimitException.class);
        server.verify();
    }
}
