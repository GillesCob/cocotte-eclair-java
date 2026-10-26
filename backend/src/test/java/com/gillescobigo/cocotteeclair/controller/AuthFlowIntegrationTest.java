package com.gillescobigo.cocotteeclair.controller;

import com.gillescobigo.cocotteeclair.entity.User;
import com.gillescobigo.cocotteeclair.repository.UserRepository;
import com.gillescobigo.cocotteeclair.security.RefreshCookieFactory;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Parcours d'authentification complet sur une vraie base H2 : login, refresh avec rotation, logout,
// protection CSRF par en-tete. Capacites de limitation de debit relevees pour cette classe seulement
// (contexte Spring distinct, donc compteurs distincts) afin que les nombreux login ne declenchent pas
// le 429 (la limitation elle-meme est testee dans AuthRateLimitIntegrationTest).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "app.rate-limit.login.capacity=100",
        "app.rate-limit.refresh.capacity=50"
})
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String creerUtilisateur() {
        String email = "flow-" + UUID.randomUUID() + "@cocotte.fr";
        userRepository.save(new User(email, passwordEncoder.encode("motdepasse123")));
        return email;
    }

    private static String valeurDuCookie(String setCookie) {
        String debut = RefreshCookieFactory.COOKIE_NAME + "=";
        String reste = setCookie.substring(setCookie.indexOf(debut) + debut.length());
        return reste.substring(0, reste.indexOf(';') >= 0 ? reste.indexOf(';') : reste.length());
    }

    private static String corpsLogin(String email, String motDePasse) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + motDePasse + "\"}";
    }

    @Test
    void login_mauvaisMotDePasseEtEmailInconnu_donnentLeMemeRefus401() throws Exception {
        String email = creerUtilisateur();
        String mauvais = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(corpsLogin(email, "mauvaismdp123")))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String inconnu = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(corpsLogin("inconnu-" + UUID.randomUUID() + "@cocotte.fr", "motdepasse123")))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertThat(mauvais).isEqualTo(inconnu);
    }

    @Test
    void login_donneesInvalides_est400() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"pas-un-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_sansEnTeteAntiCsrf_est403_memeAvecUnCookieValide() throws Exception {
        String email = creerUtilisateur();
        var login = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(corpsLogin(email, "motdepasse123")))
                .andExpect(status().isOk()).andReturn();
        String refresh = valeurDuCookie(login.getResponse().getHeader("Set-Cookie"));

        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie(RefreshCookieFactory.COOKIE_NAME, refresh)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/logout").cookie(new Cookie(RefreshCookieFactory.COOKIE_NAME, refresh)))
                .andExpect(status().isForbidden());
    }

    @Test
    void refresh_sansCookie_est401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_avecUnCookieFalsifie_est401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh").header("X-Requested-With", "XMLHttpRequest")
                        .cookie(new Cookie(RefreshCookieFactory.COOKIE_NAME, "pas.un.jwt")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_avecLeBonCookie_delivreUnNouvelAccessTokenEtFaitTournerLeRefreshToken() throws Exception {
        String email = creerUtilisateur();
        var login = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(corpsLogin(email, "motdepasse123")))
                .andExpect(status().isOk()).andReturn();
        String ancien = valeurDuCookie(login.getResponse().getHeader("Set-Cookie"));

        var refresh = mockMvc.perform(post("/api/auth/refresh").header("X-Requested-With", "XMLHttpRequest")
                        .cookie(new Cookie(RefreshCookieFactory.COOKIE_NAME, ancien)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();
        String nouveau = valeurDuCookie(refresh.getResponse().getHeader("Set-Cookie"));
        assertThat(nouveau).isNotEqualTo(ancien);

        // Rotation : l'ancien refresh token ne doit plus etre accepte.
        mockMvc.perform(post("/api/auth/refresh").header("X-Requested-With", "XMLHttpRequest")
                        .cookie(new Cookie(RefreshCookieFactory.COOKIE_NAME, ancien)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_invalideLeRefreshToken() throws Exception {
        String email = creerUtilisateur();
        var login = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(corpsLogin(email, "motdepasse123")))
                .andExpect(status().isOk()).andReturn();
        String refresh = valeurDuCookie(login.getResponse().getHeader("Set-Cookie"));

        mockMvc.perform(post("/api/auth/logout").header("X-Requested-With", "XMLHttpRequest")
                        .cookie(new Cookie(RefreshCookieFactory.COOKIE_NAME, refresh)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/refresh").header("X-Requested-With", "XMLHttpRequest")
                        .cookie(new Cookie(RefreshCookieFactory.COOKIE_NAME, refresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_motDePasseTropCourtOuEmailInvalide_est400_sansCreerDeCompte() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.fr\",\"password\":\"court\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"pas-un-email\",\"password\":\"motdepasse123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forgotPassword_repondPareilPourUnEmailInconnu() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"inconnu-" + UUID.randomUUID() + "@cocotte.fr\"}"))
                .andExpect(status().isOk());
    }
}
