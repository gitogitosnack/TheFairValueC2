package org.example.web.batch.marketdata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// @ConfigurationProperties(prefix = "marketdata.api") で EDINET / FMP の API キー等を束ねる。
// 値は環境変数（EDINET_API_KEY / FMP_API_KEY 等）経由で注入する（設計書 6 章参照）。
@Component
@ConfigurationProperties(prefix = "marketdata.api")
public class MarketDataApiProperties {

    private final Edinet edinet = new Edinet();
    private final Fmp fmp = new Fmp();

    // true の場合、EDINET・FMP・yfinance-service への実際の外部通信を行わず、
    // MockStockPriceProviderImpl / MockFinancialDataProviderImpl が返すサンプルデータを使う
    // （環境変数 MARKETDATA_API_MOCK_ENABLED、設定画面の更新ボタン・内蔵スケジューラの両方に効く）。
    private boolean mockEnabled;

    public Edinet getEdinet() {
        return edinet;
    }

    public Fmp getFmp() {
        return fmp;
    }

    public boolean isMockEnabled() {
        return mockEnabled;
    }

    public void setMockEnabled(boolean mockEnabled) {
        this.mockEnabled = mockEnabled;
    }

    public static class Edinet {
        private String apiKey;
        private String baseUrl = "https://api.edinet-fsa.go.jp/api/v2";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    public static class Fmp {
        private String apiKey;
        private String baseUrl = "https://financialmodelingprep.com/api/v3";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}
