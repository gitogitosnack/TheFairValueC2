package org.example.web.batch.marketdata.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitExceptionTest {

    @Test
    void メッセージのみのコンストラクタでメッセージを保持すること() {
        RateLimitException ex = new RateLimitException("rate limited");

        assertThat(ex.getMessage()).isEqualTo("rate limited");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void メッセージと原因を保持すること() {
        RuntimeException cause = new RuntimeException("429 Too Many Requests");

        RateLimitException ex = new RateLimitException("rate limited", cause);

        assertThat(ex.getMessage()).isEqualTo("rate limited");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
