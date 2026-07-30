package com.gillescobigo.cocotteeclair.dto;

import com.gillescobigo.cocotteeclair.entity.Recette;

import java.util.List;
import java.util.UUID;

public record RecetteResponse(
        UUID id,
        String titre,
        String description,
        Recette.Visibilite visibilite,
        UUID proprietaireId,
        UUID recetteParenteId,
        boolean estRecetteDeBase,
        List<RecetteIngredientResponse> ingredients,
        List<EtapeResponse> etapes
) {
    public static RecetteResponse from(Recette recette, List<RecetteIngredientResponse> ingredients, List<EtapeResponse> etapes) {
        return new RecetteResponse(
                recette.getId(),
                recette.getTitre(),
                recette.getDescription(),
                recette.getVisibilite(),
                recette.getProprietaire() != null ? recette.getProprietaire().getId() : null,
                recette.getRecetteParente() != null ? recette.getRecetteParente().getId() : null,
                recette.getProprietaire() == null,
                ingredients,
                etapes
        );
    }
}
