package com.gillescobigo.cocotteeclair.security;

import com.gillescobigo.cocotteeclair.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Set;

// Defense CSRF ciblee sur /refresh et /logout : ce sont les deux seules routes qui
// dependent du cookie refresh_token (SameSite=None, donc envoye meme cross-site,
// contrairement aux autres routes qui utilisent le header Authorization, deja
// immunisees par construction). Un attaquant cross-site ne peut pas ajouter ce
// header custom sans passer par du JS, qui serait de toute facon bloque par la
// politique CORS deja stricte (une seule origine autorisee, cf SecurityConfig).
// Alternative a un vrai token CSRF stateful, jugee proportionnee pour une V1.
@Component
public class CsrfHeaderCheckFilter extends OncePerRequestFilter {

    private static final Set<String> PROTECTED_PATHS = Set.of("/api/auth/refresh", "/api/auth/logout");
    private static final String REQUIRED_HEADER = "X-Requested-With";
    private static final String REQUIRED_VALUE = "XMLHttpRequest";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        boolean estUneRouteProtegee = PROTECTED_PATHS.contains(request.getRequestURI());
        boolean headerPresent = REQUIRED_VALUE.equals(request.getHeader(REQUIRED_HEADER));

        if (estUneRouteProtegee && !headerPresent) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(new ErrorResponse("Requête refusée")));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
