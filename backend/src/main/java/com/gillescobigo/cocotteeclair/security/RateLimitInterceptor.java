package com.gillescobigo.cocotteeclair.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;

// HandlerInterceptor plutot qu'un Filter servlet brut : une exception levee ici
// passe par le meme mecanisme de resolution que les controllers (contrairement a
// un Filter, execute avant le DispatcherServlet), donc RateLimitExceededException
// est bien mappee en 429 par GlobalExceptionHandler, pas besoin de dupliquer la
// serialisation JSON comme CsrfHeaderCheckFilter a du le faire.
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final AuthRateLimitService rateLimitService;
    private final Map<String, RouteLimit> limitsByPath;

    public RateLimitInterceptor(
            AuthRateLimitService rateLimitService,
            @Value("${app.rate-limit.login.capacity}") int loginCapacity,
            @Value("${app.rate-limit.login.period-minutes}") long loginPeriodMinutes,
            @Value("${app.rate-limit.register.capacity}") int registerCapacity,
            @Value("${app.rate-limit.register.period-minutes}") long registerPeriodMinutes,
            @Value("${app.rate-limit.forgot-password.capacity}") int forgotPasswordCapacity,
            @Value("${app.rate-limit.forgot-password.period-minutes}") long forgotPasswordPeriodMinutes,
            @Value("${app.rate-limit.refresh.capacity}") int refreshCapacity,
            @Value("${app.rate-limit.refresh.period-minutes}") long refreshPeriodMinutes
    ) {
        this.rateLimitService = rateLimitService;
        this.limitsByPath = Map.of(
                "/api/auth/login", new RouteLimit(loginCapacity, Duration.ofMinutes(loginPeriodMinutes)),
                "/api/auth/register", new RouteLimit(registerCapacity, Duration.ofMinutes(registerPeriodMinutes)),
                "/api/auth/forgot-password",
                new RouteLimit(forgotPasswordCapacity, Duration.ofMinutes(forgotPasswordPeriodMinutes)),
                "/api/auth/refresh", new RouteLimit(refreshCapacity, Duration.ofMinutes(refreshPeriodMinutes))
        );
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler
    ) {
        RouteLimit limit = limitsByPath.get(request.getRequestURI());
        if (limit == null) {
            return true;
        }

        // request.getRemoteAddr() suffit tant qu'aucun reverse proxy (Nginx) n'est
        // confirme devant le backend en prod ; le jour ou c'est le cas, verifier
        // explicitement que Nginx surcharge bien X-Forwarded-For avant de lui faire
        // confiance (sinon un client peut usurper son IP via ce header).
        String key = request.getRemoteAddr() + ":" + request.getRequestURI();
        rateLimitService.checkAllowed(key, limit.capacity(), limit.refillPeriod());
        return true;
    }

    private record RouteLimit(int capacity, Duration refillPeriod) {
    }
}
