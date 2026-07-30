package com.gillescobigo.cocotteeclair.dto;

import com.gillescobigo.cocotteeclair.entity.RecetteIngredient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RecetteIngredientRequest(
        @NotBlank(message = "Le nom de l'ingrédient est obligatoire")
        String ingredientNom,

        @NotNull(message = "La quantité est obligatoire")
        @Positive(message = "La quantité doit être positive")
        Double quantite,

        @NotNull(message = "L'unité est obligatoire")
        RecetteIngredient.Unite unite
) {
}
