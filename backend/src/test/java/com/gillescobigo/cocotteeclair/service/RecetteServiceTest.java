package com.gillescobigo.cocotteeclair.service;

import com.gillescobigo.cocotteeclair.dto.EtapeRequest;
import com.gillescobigo.cocotteeclair.dto.RecetteIngredientRequest;
import com.gillescobigo.cocotteeclair.dto.RecetteRequest;
import com.gillescobigo.cocotteeclair.entity.Etape;
import com.gillescobigo.cocotteeclair.entity.Ingredient;
import com.gillescobigo.cocotteeclair.entity.Recette;
import com.gillescobigo.cocotteeclair.entity.RecetteIngredient;
import com.gillescobigo.cocotteeclair.entity.User;
import com.gillescobigo.cocotteeclair.exception.ResourceNotFoundException;
import com.gillescobigo.cocotteeclair.exception.UnauthorizedException;
import com.gillescobigo.cocotteeclair.repository.EtapeRepository;
import com.gillescobigo.cocotteeclair.repository.IngredientRepository;
import com.gillescobigo.cocotteeclair.repository.RecetteIngredientRepository;
import com.gillescobigo.cocotteeclair.repository.RecetteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecetteServiceTest {

    @Mock
    private RecetteRepository recetteRepository;
    @Mock
    private IngredientRepository ingredientRepository;
    @Mock
    private RecetteIngredientRepository recetteIngredientRepository;
    @Mock
    private EtapeRepository etapeRepository;

    private RecetteService recetteService;
    private User proprietaire;
    private User autreUtilisateur;

    @BeforeEach
    void setUp() {
        recetteService = new RecetteService(recetteRepository, ingredientRepository, recetteIngredientRepository, etapeRepository);
        proprietaire = new User("proprietaire@cocotte.fr", "hash");
        autreUtilisateur = new User("autre@cocotte.fr", "hash");
    }

    @Test
    void findById_recetteInexistante_leveResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(recetteRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recetteService.findById(id, proprietaire))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findById_recettePriveeDunAutreUtilisateur_leveResourceNotFoundPasUnauthorized() {
        Recette recette = new Recette("Secrete", "desc", autreUtilisateur);
        recette.setVisibilite(Recette.Visibilite.PRIVEE);
        when(recetteRepository.findById(recette.getId())).thenReturn(Optional.of(recette));

        // 404, pas 403 : ne pas reveler qu'une recette privee existe a quelqu'un qui n'y a pas acces.
        assertThatThrownBy(() -> recetteService.findById(recette.getId(), proprietaire))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findById_recettePubliqueDunAutreUtilisateur_estVisible() {
        Recette recette = new Recette("Publique", "desc", autreUtilisateur);
        recette.setVisibilite(Recette.Visibilite.PUBLIQUE);
        when(recetteRepository.findById(recette.getId())).thenReturn(Optional.of(recette));
        when(recetteIngredientRepository.findByRecetteId(any())).thenReturn(List.of());
        when(etapeRepository.findByRecetteIdOrderByOrdreAsc(any())).thenReturn(List.of());

        var response = recetteService.findById(recette.getId(), proprietaire);

        assertThat(response.titre()).isEqualTo("Publique");
    }

    @Test
    void findById_recetteDeBaseSystemeSansProprietaire_estVisibleDeTous() {
        Recette recetteDeBase = new Recette("Recette de base", "desc", null);
        when(recetteRepository.findById(recetteDeBase.getId())).thenReturn(Optional.of(recetteDeBase));
        when(recetteIngredientRepository.findByRecetteId(any())).thenReturn(List.of());
        when(etapeRepository.findByRecetteIdOrderByOrdreAsc(any())).thenReturn(List.of());

        var response = recetteService.findById(recetteDeBase.getId(), proprietaire);

        assertThat(response.estRecetteDeBase()).isTrue();
    }

    @Test
    void update_parQuelquUnQuiNestPasProprietaire_leveUnauthorized() {
        Recette recette = new Recette("Titre", "desc", proprietaire);
        when(recetteRepository.findById(recette.getId())).thenReturn(Optional.of(recette));
        RecetteRequest request = new RecetteRequest("Nouveau titre", "desc", Recette.Visibilite.PRIVEE);

        assertThatThrownBy(() -> recetteService.update(recette.getId(), request, autreUtilisateur))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void delete_parQuelquUnQuiNestPasProprietaire_leveUnauthorized() {
        Recette recette = new Recette("Titre", "desc", proprietaire);
        when(recetteRepository.findById(recette.getId())).thenReturn(Optional.of(recette));

        assertThatThrownBy(() -> recetteService.delete(recette.getId(), autreUtilisateur))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void delete_recetteDeBaseSystemeSansProprietaire_leveUnauthorizedPourNimporteQuelUtilisateur() {
        Recette recetteDeBase = new Recette("Recette de base", "desc", null);
        when(recetteRepository.findById(recetteDeBase.getId())).thenReturn(Optional.of(recetteDeBase));

        assertThatThrownBy(() -> recetteService.delete(recetteDeBase.getId(), proprietaire))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void updateIngredient_parQuelquUnQuiNestPasProprietaire_leveUnauthorized() {
        Recette recette = new Recette("Titre", "desc", proprietaire);
        when(recetteRepository.findById(recette.getId())).thenReturn(Optional.of(recette));
        RecetteIngredientRequest request = new RecetteIngredientRequest("Farine", 200.0, RecetteIngredient.Unite.GRAMME);

        assertThatThrownBy(() -> recetteService.updateIngredient(recette.getId(), UUID.randomUUID(), request, autreUtilisateur))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void updateIngredient_ligneDuneAutreRecette_leveResourceNotFound() {
        Recette recette = new Recette("Titre", "desc", proprietaire);
        Recette autreRecette = new Recette("Autre", "desc", proprietaire);
        RecetteIngredient ligne = new RecetteIngredient(autreRecette, new Ingredient("Sucre"), 100.0, RecetteIngredient.Unite.GRAMME);
        when(recetteRepository.findById(recette.getId())).thenReturn(Optional.of(recette));
        when(recetteIngredientRepository.findById(ligne.getId())).thenReturn(Optional.of(ligne));
        RecetteIngredientRequest request = new RecetteIngredientRequest("Farine", 200.0, RecetteIngredient.Unite.GRAMME);

        assertThatThrownBy(() -> recetteService.updateIngredient(recette.getId(), ligne.getId(), request, proprietaire))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateIngredient_parLeProprietaire_metAJourLesValeurs() {
        Recette recette = new Recette("Titre", "desc", proprietaire);
        RecetteIngredient ligne = new RecetteIngredient(recette, new Ingredient("Sucre"), 100.0, RecetteIngredient.Unite.GRAMME);
        when(recetteRepository.findById(recette.getId())).thenReturn(Optional.of(recette));
        when(recetteIngredientRepository.findById(ligne.getId())).thenReturn(Optional.of(ligne));
        when(ingredientRepository.findByNomIgnoreCase("Farine")).thenReturn(Optional.of(new Ingredient("Farine")));
        RecetteIngredientRequest request = new RecetteIngredientRequest("Farine", 250.0, RecetteIngredient.Unite.KILOGRAMME);

        var response = recetteService.updateIngredient(recette.getId(), ligne.getId(), request, proprietaire);

        assertThat(response.ingredientNom()).isEqualTo("Farine");
        assertThat(response.quantite()).isEqualTo(250.0);
        assertThat(response.unite()).isEqualTo(RecetteIngredient.Unite.KILOGRAMME);
    }

    @Test
    void updateEtape_parQuelquUnQuiNestPasProprietaire_leveUnauthorized() {
        Recette recette = new Recette("Titre", "desc", proprietaire);
        when(recetteRepository.findById(recette.getId())).thenReturn(Optional.of(recette));
        EtapeRequest request = new EtapeRequest(1, "Description", null);

        assertThatThrownBy(() -> recetteService.updateEtape(recette.getId(), UUID.randomUUID(), request, autreUtilisateur))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void updateEtape_etapeDuneAutreRecette_leveResourceNotFound() {
        Recette recette = new Recette("Titre", "desc", proprietaire);
        Recette autreRecette = new Recette("Autre", "desc", proprietaire);
        Etape etape = new Etape(autreRecette, 1, "Description", null);
        when(recetteRepository.findById(recette.getId())).thenReturn(Optional.of(recette));
        when(etapeRepository.findById(etape.getId())).thenReturn(Optional.of(etape));
        EtapeRequest request = new EtapeRequest(1, "Nouvelle description", null);

        assertThatThrownBy(() -> recetteService.updateEtape(recette.getId(), etape.getId(), request, proprietaire))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateEtape_parLeProprietaire_metAJourLesValeurs() {
        Recette recette = new Recette("Titre", "desc", proprietaire);
        Etape etape = new Etape(recette, 1, "Description initiale", null);
        when(recetteRepository.findById(recette.getId())).thenReturn(Optional.of(recette));
        when(etapeRepository.findById(etape.getId())).thenReturn(Optional.of(etape));
        EtapeRequest request = new EtapeRequest(2, "Description modifiée", 15);

        var response = recetteService.updateEtape(recette.getId(), etape.getId(), request, proprietaire);

        assertThat(response.ordre()).isEqualTo(2);
        assertThat(response.description()).isEqualTo("Description modifiée");
        assertThat(response.tempsCuissonMinutes()).isEqualTo(15);
    }
}
