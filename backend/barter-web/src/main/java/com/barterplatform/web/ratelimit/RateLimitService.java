package com.barterplatform.web.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private static final long CLEANUP_INTERVAL_REQUESTS = 1_000;

    private final ConcurrentMap<String, WindowState> buckets = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong();
    private final Clock clock;

    public RateLimitService() {
        this(Clock.systemUTC());
    }

    RateLimitService(Clock clock) {
        this.clock = clock;
    }

    public RateLimitDecision tryAcquire(String ruleId, String key, RateLimitProperties.Policy policy) {
        Objects.requireNonNull(ruleId, "ruleId must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(policy, "policy must not be null");

        if (policy.getLimit() <= 0) {
            return RateLimitDecision.blocked(policy.getWindow().toSeconds());
        }

        String bucketKey = ruleId + ':' + key;
        Instant now = clock.instant();
        DecisionHolder holder = new DecisionHolder();

        buckets.compute(bucketKey, (ignored, state) -> {
            WindowState current = state;
            if (current == null || isWindowExpired(current, policy.getWindow(), now)) {
                current = new WindowState(now, 0);
            }

            if (current.count >= policy.getLimit()) {
                holder.decision = RateLimitDecision.blocked(retryAfterSeconds(current, policy.getWindow(), now));
                return current;
            }

            current.count++;
            holder.decision = RateLimitDecision.allowed();
            return current;
        });

        maybeCleanup(now);
        return holder.decision;
    }

    private boolean isWindowExpired(WindowState state, Duration window, Instant now) {
        return !now.isBefore(state.windowStartedAt.plus(window));
    }

    private long retryAfterSeconds(WindowState state, Duration window, Instant now) {
        long millis = Duration.between(now, state.windowStartedAt.plus(window)).toMillis();
        return Math.max(1, (long) Math.ceil(millis / 1000.0d));
    }

    private void maybeCleanup(Instant now) {
        if (requestCounter.incrementAndGet() % CLEANUP_INTERVAL_REQUESTS != 0) {
            return;
        }

        buckets.entrySet().removeIf(entry -> entry.getValue().windowStartedAt.plus(Duration.ofHours(1)).isBefore(now));
    }

    public record RateLimitDecision(boolean permitted, long retryAfterSeconds) {
        static RateLimitDecision allowed() {
            return new RateLimitDecision(true, 0);
        }

        static RateLimitDecision blocked(long retryAfterSeconds) {
            return new RateLimitDecision(false, retryAfterSeconds);
        }
    }

    private static final class WindowState {
        private Instant windowStartedAt;
        private int count;

        private WindowState(Instant windowStartedAt, int count) {
            this.windowStartedAt = windowStartedAt;
            this.count = count;
        }
    }

    private static final class DecisionHolder {
        private RateLimitDecision decision;
    }
}

