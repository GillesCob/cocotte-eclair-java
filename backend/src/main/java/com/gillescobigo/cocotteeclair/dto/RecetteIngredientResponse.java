package com.gillescobigo.cocotteeclair.dto;

import com.gillescobigo.cocotteeclair.entity.RecetteIngredient;

import java.util.UUID;

public record RecetteIngredientResponse(UUID id, String ingredientNom, Double quantite, RecetteIngredient.Unite unite) {
    public static RecetteIngredientResponse from(RecetteIngredient ri) {
        return new RecetteIngredientResponse(ri.getId(), ri.getIngredient().getNom(), ri.getQuantite(), ri.getUnite());
    }
}
