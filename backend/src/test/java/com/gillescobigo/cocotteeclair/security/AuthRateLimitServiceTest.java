package com.gillescobigo.cocotteeclair.security;

import com.gillescobigo.cocotteeclair.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthRateLimitServiceTest {

    private final AuthRateLimitService rateLimitService = new AuthRateLimitService();

    @Test
    void checkAllowed_sousLeQuota_neLeveRien() {
        assertThatCode(() -> {
            rateLimitService.checkAllowed("ip-test-1", 3, Duration.ofMinutes(1));
            rateLimitService.checkAllowed("ip-test-1", 3, Duration.ofMinutes(1));
            rateLimitService.checkAllowed("ip-test-1", 3, Duration.ofMinutes(1));
        }).doesNotThrowAnyException();
    }

    @Test
    void checkAllowed_quotaAtteint_leveRateLimitExceeded() {
        rateLimitService.checkAllowed("ip-test-2", 2, Duration.ofMinutes(1));
        rateLimitService.checkAllowed("ip-test-2", 2, Duration.ofMinutes(1));

        assertThatThrownBy(() -> rateLimitService.checkAllowed("ip-test-2", 2, Duration.ofMinutes(1)))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void checkAllowed_clesDifferentes_sontIndependantes() {
        rateLimitService.checkAllowed("ip-test-3a", 1, Duration.ofMinutes(1));

        assertThatCode(() -> rateLimitService.checkAllowed("ip-test-3b", 1, Duration.ofMinutes(1)))
                .doesNotThrowAnyException();
    }
}
