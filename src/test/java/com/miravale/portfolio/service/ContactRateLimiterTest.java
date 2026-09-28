package com.miravale.portfolio.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContactRateLimiterTest {
    @Test
    void limitsEachClientToThreeMessagesPerWindow() {
        ContactRateLimiter rateLimiter = new ContactRateLimiter();

        assertThat(rateLimiter.tryAcquire("192.0.2.1")).isTrue();
        assertThat(rateLimiter.tryAcquire("192.0.2.1")).isTrue();
        assertThat(rateLimiter.tryAcquire("192.0.2.1")).isTrue();
        assertThat(rateLimiter.tryAcquire("192.0.2.1")).isFalse();
        assertThat(rateLimiter.tryAcquire("192.0.2.2")).isTrue();
    }
}
