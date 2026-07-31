package com.gillescobigo.cocotteeclair.security;

import com.gillescobigo.cocotteeclair.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// Compteurs en memoire (ConcurrentHashMap), valable pour un VPS mono-instance.
// Ne survivrait pas a un scaling horizontal (plusieurs instances de l'appli ne
// partageraient pas ces compteurs) : a revoir avec un backend partage (Redis) le
// jour ou ce scaling devient reel, pas avant, pour une V1.
@Service
public class AuthRateLimitService {

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public void checkAllowed(String key, int capacity, Duration refillPeriod) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket(capacity, refillPeriod));
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException("Trop de tentatives, réessayez dans quelques minutes");
        }
    }

    private Bucket newBucket(int capacity, Duration refillPeriod) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(capacity, refillPeriod));
        return Bucket.builder().addLimit(limit).build();
    }
}
