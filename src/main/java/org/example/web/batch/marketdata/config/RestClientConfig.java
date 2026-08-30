package org.example.web.batch.marketdata.config;

import java.time.Duration;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

// タイムアウト・リトライ込みの HTTP クライアント Bean 定義。
// StockPriceProvider / FinancialDataProvider の各実装（yfinance-service・FMP・EDINET 向け）から利用する。
// 429 等リトライで復帰し得るエラーの再試行自体は Spring Batch の Step 側（faultTolerant().retry()）が
// 担うため、ここではタイムアウトの共通設定のみを行う（設計書 7 章参照）。
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder marketDataRestClientBuilder() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(5))
                .withReadTimeout(Duration.ofSeconds(20));
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(settings);
        return RestClient.builder().requestFactory(requestFactory);
    }
}
