package org.example.web.batch.marketdata.client;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import org.example.web.batch.marketdata.client.dto.FinancialStatementData;
import org.example.web.batch.marketdata.config.MarketDataApiProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// FinancialDataProvider の米国株向け実装。Financial Modeling Prep（FMP）の
// stable 版の income-statement / balance-sheet-statement / cash-flow-statement（?symbol=...&period=quarter）を
// 取得する（設計書 5.2 参照）。stable 版でフィールド名が変わった項目は旧 v3 名も @JsonAlias で受け付ける。
@Component("fmpFinancialDataProvider")
public class FmpFinancialDataProviderImpl implements FinancialDataProvider {

    private final RestClient restClient;
    private final MarketDataApiProperties apiProperties;

    public FmpFinancialDataProviderImpl(
            RestClient.Builder marketDataRestClientBuilder,
            MarketDataApiProperties apiProperties) {
        this.apiProperties = apiProperties;
        this.restClient = marketDataRestClientBuilder.clone()
                .baseUrl(apiProperties.getFmp().getBaseUrl())
                .build();
    }

    @Override
    public FinancialStatementData fetchLatestStatement(String code) {
        FmpIncomeStatement income = fetchFirst("/income-statement", code, FmpIncomeStatement[].class);
        if (income == null) {
            return null;
        }
        FmpBalanceSheet balance = fetchFirst("/balance-sheet-statement", code, FmpBalanceSheet[].class);
        FmpCashFlow cashFlow = fetchFirst("/cash-flow-statement", code, FmpCashFlow[].class);

        Integer fiscalYear = income.fiscalYear() != null ? Integer.valueOf(income.fiscalYear()) : null;
        String fiscalQuarter = income.period();

        BigDecimal bps = (balance != null && balance.totalStockholdersEquity() != null
                && income.weightedAverageShsOut() != null && income.weightedAverageShsOut().signum() != 0)
                ? balance.totalStockholdersEquity().divide(income.weightedAverageShsOut(), 4, RoundingMode.HALF_UP)
                : null;

        return new FinancialStatementData(
                fiscalYear,
                fiscalQuarter,
                income.date(),
                income.revenue(),
                income.operatingIncome(),
                income.netIncome(),
                income.eps(),
                bps,
                cashFlow != null ? cashFlow.operatingCashFlow() : null,
                cashFlow != null ? cashFlow.investingCashFlow() : null,
                cashFlow != null ? cashFlow.freeCashFlow() : null,
                balance != null ? balance.totalAssets() : null,
                balance != null ? balance.totalLiabilities() : null,
                balance != null ? balance.cashAndCashEquivalents() : null,
                income.grossProfit(),
                income.sellingGeneralAndAdministrativeExpenses(),
                income.operatingIncome(),
                balance != null ? balance.totalStockholdersEquity() : null,
                balance != null ? balance.inventory() : null,
                balance != null ? balance.netReceivables() : null,
                cashFlow != null ? cashFlow.financingCashFlow() : null,
                income.interestExpense(),
                cashFlow != null ? cashFlow.dividendsPaid() : null);
    }

    private <T> T fetchFirst(String path, String code, Class<T[]> arrayType) {
        T[] response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(path)
                            .queryParam("symbol", code)
                            .queryParam("period", "quarter")
                            .queryParam("limit", "1")
                            .queryParam("apikey", apiProperties.getFmp().getApiKey())
                            .build())
                    .retrieve()
                    .body(arrayType);
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new RateLimitException("FMP financial statement API rate limited: path=" + path + ", code=" + code, e);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
        return (response == null || response.length == 0) ? null : response[0];
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FmpIncomeStatement(
            LocalDate date,
            String period,
            @JsonAlias("calendarYear") String fiscalYear,
            BigDecimal revenue,
            BigDecimal grossProfit,
            BigDecimal operatingIncome,
            BigDecimal netIncome,
            BigDecimal eps,
            BigDecimal weightedAverageShsOut,
            BigDecimal sellingGeneralAndAdministrativeExpenses,
            BigDecimal interestExpense) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FmpBalanceSheet(
            BigDecimal totalAssets,
            BigDecimal totalLiabilities,
            BigDecimal totalStockholdersEquity,
            BigDecimal cashAndCashEquivalents,
            BigDecimal inventory,
            BigDecimal netReceivables) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FmpCashFlow(
            BigDecimal operatingCashFlow,
            @JsonAlias("netCashUsedForInvestingActivites") BigDecimal netCashProvidedByInvestingActivities,
            @JsonAlias("netCashUsedProvidedByFinancingActivities") BigDecimal netCashProvidedByFinancingActivities,
            BigDecimal freeCashFlow,
            @JsonAlias("dividendsPaid") BigDecimal netDividendsPaid) {

        BigDecimal investingCashFlow() {
            return netCashProvidedByInvestingActivities;
        }

        BigDecimal financingCashFlow() {
            return netCashProvidedByFinancingActivities;
        }

        BigDecimal dividendsPaid() {
            return netDividendsPaid;
        }
    }
}
