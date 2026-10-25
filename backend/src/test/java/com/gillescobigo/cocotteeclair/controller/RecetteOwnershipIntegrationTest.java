package com.gillescobigo.cocotteeclair.controller;

import com.gillescobigo.cocotteeclair.entity.Recette;
import com.gillescobigo.cocotteeclair.entity.User;
import com.gillescobigo.cocotteeclair.repository.RecetteRepository;
import com.gillescobigo.cocotteeclair.repository.UserRepository;
import com.gillescobigo.cocotteeclair.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Droits d'acces sur une vraie base H2 et toute la chaine de securite (filtre JWT, SecurityConfig,
// GlobalExceptionHandler), sans mock : meme principe que RecetteControllerIntegrationTest.
// Les utilisateurs sont crees via les repositories et leur jeton est genere par JwtService : on
// n'appelle pas /api/auth/register ici, car la limite de 3 inscriptions par heure et par IP est
// partagee par tous les tests qui reutilisent le meme contexte Spring.
// Test ecrit en couverture du point 2 du harnais (droits d'acces et flux metier principal).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecetteOwnershipIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RecetteRepository recetteRepository;
    @Autowired
    private JwtService jwtService;

    private User alice;
    private User bob;
    private String aliceToken;
    private String bobToken;

    @BeforeEach
    void creerDeuxUtilisateurs() {
        alice = userRepository.save(new User("alice-" + UUID.randomUUID() + "@cocotte.fr", "hash"));
        bob = userRepository.save(new User("bob-" + UUID.randomUUID() + "@cocotte.fr", "hash"));
        aliceToken = "Bearer " + jwtService.generateAccessToken(alice.getId());
        bobToken = "Bearer " + jwtService.generateAccessToken(bob.getId());
    }

    private Recette recetteDeAlice(Recette.Visibilite visibilite) {
        Recette recette = new Recette("Recette d'Alice", "desc", alice);
        recette.setVisibilite(visibilite);
        return recetteRepository.save(recette);
    }

    private Recette recetteDeBase() {
        Recette recette = new Recette("Recette de base " + UUID.randomUUID(), "desc", null);
        recette.setVisibilite(Recette.Visibilite.PUBLIQUE);
        return recetteRepository.save(recette);
    }

    // --- Authentification obligatoire ---

    @Test
    void sansJeton_lesRoutesRecettesSontRefusees() throws Exception {
        UUID id = recetteDeAlice(Recette.Visibilite.PUBLIQUE).getId();
        mockMvc.perform(get("/api/recettes")).andExpect(status().is4xxClientError()).andExpect(status().is(org.hamcrest.Matchers.oneOf(401, 403)));
        mockMvc.perform(get("/api/recettes/" + id)).andExpect(status().is(org.hamcrest.Matchers.oneOf(401, 403)));
        mockMvc.perform(delete("/api/recettes/" + id)).andExpect(status().is(org.hamcrest.Matchers.oneOf(401, 403)));
        mockMvc.perform(post("/api/recettes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\":\"x\",\"visibilite\":\"PRIVEE\"}"))
                .andExpect(status().is(org.hamcrest.Matchers.oneOf(401, 403)));
    }

    @Test
    void jetonInvalideOuSigneAvecUnAutreSecret_estRefuse() throws Exception {
        mockMvc.perform(get("/api/recettes").header("Authorization", "Bearer pas.un.jwt"))
                .andExpect(status().is(org.hamcrest.Matchers.oneOf(401, 403)));
    }

    @Test
    void jetonDunUtilisateurSupprime_estRefuse() throws Exception {
        String token = "Bearer " + jwtService.generateAccessToken(UUID.randomUUID());
        mockMvc.perform(get("/api/recettes").header("Authorization", token))
                .andExpect(status().is(org.hamcrest.Matchers.oneOf(401, 403)));
    }

    @Test
    void leRefreshTokenNEstPasAccepteCommeAccessToken() throws Exception {
        String refresh = "Bearer " + jwtService.generateRefreshToken(alice.getId());
        mockMvc.perform(get("/api/recettes").header("Authorization", refresh))
                .andExpect(status().is(org.hamcrest.Matchers.oneOf(401, 403)));
    }

    @Test
    void healthEstPublic() throws Exception {
        mockMvc.perform(get("/health")).andExpect(status().isOk());
    }

    // --- Lecture : visibilite ---

    @Test
    void recettePriveeDAutrui_est404EnLectureEtAbsenteDeLaListe() throws Exception {
        Recette privee = recetteDeAlice(Recette.Visibilite.PRIVEE);

        mockMvc.perform(get("/api/recettes/" + privee.getId()).header("Authorization", bobToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/recettes").header("Authorization", bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(privee.getId().toString()))));
    }

    @Test
    void proprietaire_voitSaRecettePriveeDansLaListeEtEnDetail() throws Exception {
        Recette privee = recetteDeAlice(Recette.Visibilite.PRIVEE);

        mockMvc.perform(get("/api/recettes/" + privee.getId()).header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titre").value("Recette d'Alice"));
        mockMvc.perform(get("/api/recettes").header("Authorization", aliceToken))
                .andExpect(jsonPath("$[*].id", hasItem(privee.getId().toString())));
    }

    @Test
    void recettePubliqueEtRecetteDeBase_sontVisiblesDeTous() throws Exception {
        Recette publique = recetteDeAlice(Recette.Visibilite.PUBLIQUE);
        Recette base = recetteDeBase();

        mockMvc.perform(get("/api/recettes/" + publique.getId()).header("Authorization", bobToken)).andExpect(status().isOk());
        mockMvc.perform(get("/api/recettes/" + base.getId()).header("Authorization", bobToken)).andExpect(status().isOk());
        mockMvc.perform(get("/api/recettes").header("Authorization", bobToken))
                .andExpect(jsonPath("$[*].id", hasItem(publique.getId().toString())))
                .andExpect(jsonPath("$[*].id", hasItem(base.getId().toString())));
    }

    @Test
    void recetteInexistante_est404() throws Exception {
        mockMvc.perform(get("/api/recettes/" + UUID.randomUUID()).header("Authorization", aliceToken))
                .andExpect(status().isNotFound());
    }

    // --- Ecriture : seul le proprietaire ---

    @Test
    void modifierOuSupprimerLaRecetteDAutrui_estRefuse403_meme_si_elle_est_publique() throws Exception {
        Recette publique = recetteDeAlice(Recette.Visibilite.PUBLIQUE);
        String corps = "{\"titre\":\"Piratee\",\"description\":null,\"visibilite\":\"PUBLIQUE\"}";

        mockMvc.perform(put("/api/recettes/" + publique.getId()).header("Authorization", bobToken)
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/recettes/" + publique.getId()).header("Authorization", bobToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/recettes/" + publique.getId()).header("Authorization", aliceToken))
                .andExpect(jsonPath("$.titre").value("Recette d'Alice"));
    }

    @Test
    void ajouterIngredientOuEtapeSurLaRecetteDAutrui_estRefuse403() throws Exception {
        Recette publique = recetteDeAlice(Recette.Visibilite.PUBLIQUE);

        mockMvc.perform(post("/api/recettes/" + publique.getId() + "/ingredients").header("Authorization", bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredientNom\":\"Sel\",\"quantite\":1,\"unite\":\"PINCEE\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/recettes/" + publique.getId() + "/etapes").header("Authorization", bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ordre\":1,\"description\":\"Melanger\",\"tempsCuissonMinutes\":null}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void recetteDeBase_nEstModifiableNiSupprimablePourPersonne() throws Exception {
        Recette base = recetteDeBase();
        mockMvc.perform(delete("/api/recettes/" + base.getId()).header("Authorization", aliceToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/recettes/" + base.getId()).header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\":\"x\",\"description\":null,\"visibilite\":\"PRIVEE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void creation_rattacheLaRecetteAuProprietaireDuJetonEtNonAUnIdentifiantDuCorps() throws Exception {
        var result = mockMvc.perform(post("/api/recettes").header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\":\"Crepes\",\"description\":null,\"visibilite\":\"PRIVEE\",\"proprietaireId\":\"" + bob.getId() + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String id = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/recettes/" + id).header("Authorization", bobToken)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/recettes/" + id).header("Authorization", aliceToken)).andExpect(status().isOk());
    }

    // --- Validation ---

    @Test
    void creation_sansTitreOuSansVisibilite_est400() throws Exception {
        mockMvc.perform(post("/api/recettes").header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"titre\":\"\",\"visibilite\":\"PRIVEE\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/recettes").header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"titre\":\"Ok\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creation_avecCorpsVideOuVisibiliteInconnue_estRefusee() throws Exception {
        mockMvc.perform(post("/api/recettes").header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/recettes").header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"titre\":\"Ok\",\"visibilite\":\"SECRETE\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/recettes").header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{pas du json"))
                .andExpect(status().isBadRequest());
    }

    // --- Copie ---

    @Test
    void copierUneRecettePublique_creeUneCopiePriveeAppartenantAuDemandeur() throws Exception {
        Recette publique = recetteDeAlice(Recette.Visibilite.PUBLIQUE);

        var result = mockMvc.perform(post("/api/recettes/" + publique.getId() + "/copy").header("Authorization", bobToken))
                .andExpect(status().isCreated())
                .andReturn();
        String copieId = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/recettes/" + copieId).header("Authorization", bobToken)).andExpect(status().isOk());
        mockMvc.perform(get("/api/recettes/" + copieId).header("Authorization", aliceToken)).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/recettes/" + copieId).header("Authorization", bobToken)).andExpect(status().isNoContent());
    }

    @Test
    void copierUneRecettePriveeDAutrui_est404() throws Exception {
        Recette privee = recetteDeAlice(Recette.Visibilite.PRIVEE);
        mockMvc.perform(post("/api/recettes/" + privee.getId() + "/copy").header("Authorization", bobToken))
                .andExpect(status().isNotFound());
    }
}
