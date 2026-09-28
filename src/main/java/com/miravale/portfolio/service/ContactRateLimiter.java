package com.miravale.portfolio.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

@Component
public class ContactRateLimiter {
    private static final int MAX_REQUESTS = 3;
    private static final int MAX_TRACKED_CLIENTS = 10_000;
    private static final long WINDOW_MILLIS = Duration.ofMinutes(15).toMillis();

    private final Map<String, ArrayDeque<Long>> attemptsByClient = new HashMap<>();

    public synchronized boolean tryAcquire(String clientAddress) {
        long now = System.currentTimeMillis();
        removeExpiredAttempts(now);

        String clientKey = clientAddress == null ? "unknown" : clientAddress;
        ArrayDeque<Long> attempts = attemptsByClient.get(clientKey);
        if (attempts == null) {
            if (attemptsByClient.size() >= MAX_TRACKED_CLIENTS) {
                return false;
            }
            attempts = new ArrayDeque<>();
            attemptsByClient.put(clientKey, attempts);
        }

        if (attempts.size() >= MAX_REQUESTS) {
            return false;
        }
        attempts.addLast(now);
        return true;
    }

    private void removeExpiredAttempts(long now) {
        long cutoff = now - WINDOW_MILLIS;
        attemptsByClient.values().forEach(attempts -> {
            while (!attempts.isEmpty() && attempts.getFirst() < cutoff) {
                attempts.removeFirst();
            }
        });
        attemptsByClient.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}
