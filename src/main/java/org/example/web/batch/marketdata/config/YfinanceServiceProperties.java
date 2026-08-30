package org.example.web.batch.marketdata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// @ConfigurationProperties(prefix = "marketdata.yfinance-service") で
// python-services/yfinance-service のベース URL（環境変数 YFINANCE_SERVICE_BASE_URL）を保持する（設計書 3.3・6 章参照）。
@Component
@ConfigurationProperties(prefix = "marketdata.yfinance-service")
public class YfinanceServiceProperties {

    private String baseUrl = "http://localhost:8081";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
