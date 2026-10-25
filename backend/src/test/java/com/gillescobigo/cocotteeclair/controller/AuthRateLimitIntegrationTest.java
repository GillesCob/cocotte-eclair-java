package com.gillescobigo.cocotteeclair.controller;

import com.gillescobigo.cocotteeclair.entity.User;
import com.gillescobigo.cocotteeclair.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Limitation de debit de bout en bout (intercepteur + GlobalExceptionHandler -> 429). Classe dediee :
// ses proprietes creent un contexte Spring a part, donc des compteurs que les autres tests ne touchent pas.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "app.rate-limit.login.capacity=3")
class AuthRateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void login_apresLaCapaciteAutorisee_laTentativeSuivanteEst429_memeAvecLeBonMotDePasse() throws Exception {
        userRepository.save(new User("ratelimit@cocotte.fr", passwordEncoder.encode("motdepasse123")));
        String mauvais = "{\"email\":\"ratelimit@cocotte.fr\",\"password\":\"mauvaismdp123\"}";
        String bon = "{\"email\":\"ratelimit@cocotte.fr\",\"password\":\"motdepasse123\"}";

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(mauvais))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(bon))
                .andExpect(status().isTooManyRequests());
    }
}
