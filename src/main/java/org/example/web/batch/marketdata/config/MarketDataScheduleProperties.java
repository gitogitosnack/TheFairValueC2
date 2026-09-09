package org.example.web.batch.marketdata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// @ConfigurationProperties(prefix = "marketdata.schedule") で各バッチの cron 式・内蔵スケジューラの
// On/Off を束ねる（MARKETDATA_SCHEDULER_ENABLED / MARKETDATA_QUOTE_CRON_JP / MARKETDATA_QUOTE_CRON_US /
// MARKETDATA_FINANCIAL_CRON、設計書 6 章参照）。
@Component
@ConfigurationProperties(prefix = "marketdata.schedule")
public class MarketDataScheduleProperties {

    private boolean schedulerEnabled = false;
    private String quoteCronJp = "0 0 16 * * MON-FRI";
    private String quoteCronUs = "0 30 6 * * MON-FRI";
    private String financialCron = "0 0 20 * * *";

    public boolean isSchedulerEnabled() {
        return schedulerEnabled;
    }

    public void setSchedulerEnabled(boolean schedulerEnabled) {
        this.schedulerEnabled = schedulerEnabled;
    }

    public String getQuoteCronJp() {
        return quoteCronJp;
    }

    public void setQuoteCronJp(String quoteCronJp) {
        this.quoteCronJp = quoteCronJp;
    }

    public String getQuoteCronUs() {
        return quoteCronUs;
    }

    public void setQuoteCronUs(String quoteCronUs) {
        this.quoteCronUs = quoteCronUs;
    }

    public String getFinancialCron() {
        return financialCron;
    }

    public void setFinancialCron(String financialCron) {
        this.financialCron = financialCron;
    }
}
