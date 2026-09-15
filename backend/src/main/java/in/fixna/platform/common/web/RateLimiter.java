package in.fixna.platform.common.web;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory sliding-window rate limiter. Fixed max hits per key per window;
 * expired buckets self-prune, with an opportunistic sweep when the map grows.
 * A single instance is shared app-wide (no external state), which is safe for
 * the MVP monolith; keys combine caller IP with route (see RateLimitFilter).
 */
public class RateLimiter {

    private final int maxHits;
    private final long windowNanos;
    private final ConcurrentHashMap<String, Deque<Long>> buckets = new ConcurrentHashMap<>();
    private final AtomicLong calls = new AtomicLong();

    RateLimiter(int maxHits, Duration window) {
        if (maxHits <= 0) {
            throw new IllegalArgumentException("maxHits must be positive");
        }
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be a positive duration");
        }
        this.maxHits = maxHits;
        this.windowNanos = window.toNanos();
    }

    /** Allows the hit when the key is below the quota. */
    public boolean allow(String key) {
        Deque<Long> bucket = buckets.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        long now = System.nanoTime();
        synchronized (bucket) {
            prune(bucket, now);
            if (bucket.size() >= maxHits) {
                return false;
            }
            bucket.addLast(now);
            if (calls.incrementAndGet() % 256 == 0) {
                sweep(now);
            }
            return true;
        }
    }

    /** Full seconds the caller should wait before retrying (0 when allowed). */
    public long retryAfterSeconds(String key) {
        Deque<Long> bucket = buckets.get(key);
        if (bucket == null) {
            return 0;
        }
        synchronized (bucket) {
            if (bucket.isEmpty()) {
                return 0;
            }
            long oldest = bucket.peekFirst();
            long waitNanos = windowNanos - (System.nanoTime() - oldest);
            return waitNanos <= 0 ? 0 : Math.max(1, (waitNanos + 999_999_999L) / 1_000_000_000L);
        }
    }

    private void prune(Deque<Long> bucket, long now) {
        while (!bucket.isEmpty() && now - bucket.peekFirst() >= windowNanos) {
            bucket.removeFirst();
        }
    }

    private void sweep(long now) {
        buckets.entrySet().removeIf(entry -> {
            Deque<Long> bucket = entry.getValue();
            synchronized (bucket) {
                prune(bucket, now);
                return bucket.isEmpty();
            }
        });
    }
}
