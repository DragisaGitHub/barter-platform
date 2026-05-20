package com.barterplatform.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class RateLimitServiceTest {

    @Test
    void shouldAllowRequestsUntilLimitThenBlockUntilWindowResets() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-20T10:00:00Z"));
        RateLimitService service = new RateLimitService(clock);
        RateLimitProperties.Policy policy = new RateLimitProperties.Policy(2, Duration.ofSeconds(10));

        assertThat(service.tryAcquire("auth-login", "ip:127.0.0.1", policy).permitted()).isTrue();
        assertThat(service.tryAcquire("auth-login", "ip:127.0.0.1", policy).permitted()).isTrue();

        RateLimitService.RateLimitDecision blocked = service.tryAcquire("auth-login", "ip:127.0.0.1", policy);
        assertThat(blocked.permitted()).isFalse();
        assertThat(blocked.retryAfterSeconds()).isEqualTo(10);

        clock.advance(Duration.ofSeconds(10));

        assertThat(service.tryAcquire("auth-login", "ip:127.0.0.1", policy).permitted()).isTrue();
    }

    @Test
    void shouldKeepSeparateBucketsByRuleAndKey() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-20T10:00:00Z"));
        RateLimitService service = new RateLimitService(clock);
        RateLimitProperties.Policy policy = new RateLimitProperties.Policy(1, Duration.ofMinutes(1));

        assertThat(service.tryAcquire("auth-login", "ip:127.0.0.1", policy).permitted()).isTrue();
        assertThat(service.tryAcquire("auth-login", "ip:127.0.0.1", policy).permitted()).isFalse();
        assertThat(service.tryAcquire("auth-login", "ip:127.0.0.2", policy).permitted()).isTrue();
        assertThat(service.tryAcquire("auth-register", "ip:127.0.0.1", policy).permitted()).isTrue();
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}

