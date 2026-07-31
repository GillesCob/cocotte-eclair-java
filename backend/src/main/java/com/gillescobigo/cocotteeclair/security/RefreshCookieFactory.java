package com.gillescobigo.cocotteeclair.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

// SameSite=Lax et non None : le frontend (cocotteeclair.gillescobigo.com) et le
// backend (cocotteeclair-api.gillescobigo.com) sont bien des ORIGINES differentes
// (sous-domaines distincts), mais partagent le meme domaine enregistrable
// (gillescobigo.com) -> le SameSite cookie policy les considere comme le meme SITE,
// pas cross-site. Lax suffit donc, et presente deux avantages sur None : (1) ne
// requiert pas Secure=true (None l'exige, et un cookie None sans Secure est purement
// et silencieusement rejete par les navigateurs modernes, y compris en local sur
// http://localhost, bug reel constate en verification manuelle) ; (2) offre une
// vraie protection CSRF native (un POST cross-site forge n'envoie jamais un cookie
// Lax), en complement du controle d'en-tete deja en place sur /refresh et /logout.
// app.cookie.secure reste configurable (false en dev http, true en prod https),
// toujours recommande mais plus jamais bloquant pour l'acceptation du cookie.
@Component
public class RefreshCookieFactory {

    public static final String COOKIE_NAME = "refresh_token";

    private final boolean secureCookie;
    private final Duration refreshTtl;

    public RefreshCookieFactory(
            @Value("${app.cookie.secure}") boolean secureCookie,
            @Value("${app.jwt.refresh-ttl-days}") long refreshTtlDays
    ) {
        this.secureCookie = secureCookie;
        this.refreshTtl = Duration.ofDays(refreshTtlDays);
    }

    public ResponseCookie build(String refreshToken) {
        return ResponseCookie.from(COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(refreshTtl)
                .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(0)
                .build();
    }
}
