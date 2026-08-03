package com.gillescobigo.cocotteeclair.controller;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @SpringBootTest + vraie base H2 (pas de repository mocke) : seul niveau de test
// capable de detecter une violation de contrainte referentielle reelle, invisible
// pour un test unitaire avec des repositories Mockito (aucune contrainte en jeu).
// Regression du 03/08 : suppression d'une recette avec ingredients/etapes echouait
// en 500 (FK recette_id sur etapes/recette_ingredients, pas de cascade), passait
// inapercu en test unitaire car les mocks ne voient jamais la contrainte.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecetteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndGetToken() throws Exception {
        String email = "integration-recette-" + UUID.randomUUID() + "@cocotte.fr";
        var result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"motdepasse123\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    void delete_recetteAvecIngredientEtEtape_supprimeSansViolationDeContrainte() throws Exception {
        String token = "Bearer " + registerAndGetToken();

        var createResult = mockMvc.perform(post("/api/recettes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\":\"Test integration\",\"description\":null,\"visibilite\":\"PRIVEE\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String recetteId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/recettes/" + recetteId + "/ingredients")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredientNom\":\"Sel\",\"quantite\":1,\"unite\":\"PINCEE\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/recettes/" + recetteId + "/etapes")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ordre\":1,\"description\":\"Melanger\",\"tempsCuissonMinutes\":null}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/recettes/" + recetteId).header("Authorization", token))
                .andExpect(status().isNoContent());
    }
}
