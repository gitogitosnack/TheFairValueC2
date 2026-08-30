package org.example.web.batch.marketdata.client;

// 外部 API が 429（Too Many Requests）を返した場合に各 Provider 実装から throw される。
// dailyQuoteSyncStep / usFinancialStatementStep 等の .faultTolerant().retry(RateLimitException.class) が
// これを捕捉して自動リトライする（設計書 4.2・7 章参照）。
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }

    public RateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
