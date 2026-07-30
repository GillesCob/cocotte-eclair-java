package com.gillescobigo.cocotteeclair.dto;

import com.gillescobigo.cocotteeclair.entity.Recette;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecetteRequest(
        @NotBlank(message = "Le titre est obligatoire")
        String titre,

        String description,

        @NotNull(message = "La visibilité est obligatoire")
        Recette.Visibilite visibilite
) {
}
