package com.gillescobigo.cocotteeclair.controller;

import com.gillescobigo.cocotteeclair.dto.AccessTokenResponse;
import com.gillescobigo.cocotteeclair.dto.AuthResponse;
import com.gillescobigo.cocotteeclair.dto.ForgotPasswordRequest;
import com.gillescobigo.cocotteeclair.dto.LoginRequest;
import com.gillescobigo.cocotteeclair.dto.RegisterRequest;
import com.gillescobigo.cocotteeclair.dto.ResetPasswordRequest;
import com.gillescobigo.cocotteeclair.exception.InvalidTokenException;
import com.gillescobigo.cocotteeclair.security.RefreshCookieFactory;
import com.gillescobigo.cocotteeclair.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshCookieFactory refreshCookieFactory;

    public AuthController(AuthService authService, RefreshCookieFactory refreshCookieFactory) {
        this.authService = authService;
        this.refreshCookieFactory = refreshCookieFactory;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AccessTokenResponse register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        return respondWithRefreshCookie(authService.register(request), response);
    }

    @PostMapping("/login")
    public AccessTokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return respondWithRefreshCookie(authService.login(request), response);
    }

    @PostMapping("/refresh")
    public AccessTokenResponse refresh(
            @CookieValue(name = RefreshCookieFactory.COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null) {
            throw new InvalidTokenException("Aucune session active");
        }
        return respondWithRefreshCookie(authService.refresh(refreshToken), response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    public void logout(
            @CookieValue(name = RefreshCookieFactory.COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookieFactory.clear().toString());
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.OK)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.OK)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
    }

    // Le refresh token brut n'est jamais renvoye dans le corps JSON : uniquement en
    // cookie httpOnly, illisible en JS. Seul l'access token est expose au client.
    private AccessTokenResponse respondWithRefreshCookie(AuthResponse tokens, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookieFactory.build(tokens.refreshToken()).toString());
        return new AccessTokenResponse(tokens.accessToken());
    }
}
