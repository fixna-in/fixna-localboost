package in.fixna.platform.common.web;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sliding-window limiter: allow, exhaust, reset, isolate by key. */
class RateLimiterTest {

    @Test
    void allowsUpToQuotaThenDenies() {
        RateLimiter limiter = new RateLimiter(3, Duration.ofMinutes(1));

        assertThat(limiter.allow("k")).isTrue();
        assertThat(limiter.allow("k")).isTrue();
        assertThat(limiter.allow("k")).isTrue();
        assertThat(limiter.allow("k")).isFalse();
    }

    @Test
    void deniesAreQuotasPerKey() {
        RateLimiter limiter = new RateLimiter(1, Duration.ofMinutes(1));

        assertThat(limiter.allow("a")).isTrue();
        assertThat(limiter.allow("a")).isFalse();
        assertThat(limiter.allow("b")).isTrue();
    }

    @Test
    void windowExpiryResetsQuota() throws Exception {
        RateLimiter limiter = new RateLimiter(1, Duration.ofMillis(50));

        assertThat(limiter.allow("k")).isTrue();
        assertThat(limiter.allow("k")).isFalse();
        Thread.sleep(80);
        assertThat(limiter.allow("k")).isTrue();
    }

    @Test
    void retryAfterHintIsReasonable() {
        RateLimiter limiter = new RateLimiter(1, Duration.ofMinutes(1));

        assertThat(limiter.retryAfterSeconds("absent")).isZero();
        assertThat(limiter.allow("k")).isTrue();
        assertThat(limiter.retryAfterSeconds("k")).isBetween(1L, 60L);
        assertThat(limiter.allow("k")).isFalse();
    }

    @Test
    void constructorRejectsInvalidConfig() {
        assertThatThrownBy(() -> new RateLimiter(0, Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RateLimiter(3, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}