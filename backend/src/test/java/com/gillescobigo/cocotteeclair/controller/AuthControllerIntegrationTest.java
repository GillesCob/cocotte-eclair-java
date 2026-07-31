package com.gillescobigo.cocotteeclair.controller;

import com.gillescobigo.cocotteeclair.entity.User;
import com.gillescobigo.cocotteeclair.repository.UserRepository;
import com.gillescobigo.cocotteeclair.security.RefreshCookieFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @Transactional : le user de test cree ici est annule (rollback) en fin de test,
// jamais persiste reellement en base, meme logique que la regle "ne jamais laisser
// de compte de test residuel" mais sans avoir besoin de nettoyage manuel.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void login_succes_poseUnCookieHttpOnlyEtNeRenvoiePasLeRefreshTokenEnJson() throws Exception {
        User user = new User("integration-test@cocotte.fr", passwordEncoder.encode("motdepasse123"));
        userRepository.save(user);

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"integration-test@cocotte.fr\",\"password\":\"motdepasse123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();

        String setCookieHeader = result.getResponse().getHeader("Set-Cookie");

        assertThat(setCookieHeader).isNotNull();
        assertThat(setCookieHeader).contains(RefreshCookieFactory.COOKIE_NAME);
        assertThat(setCookieHeader).containsIgnoringCase("HttpOnly");
        assertThat(setCookieHeader).contains("SameSite=Lax");
    }
}
