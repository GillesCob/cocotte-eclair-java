package com.gillescobigo.cocotteeclair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EtapeRequest(
        @NotNull(message = "L'ordre est obligatoire")
        @Positive(message = "L'ordre doit être positif")
        Integer ordre,

        @NotBlank(message = "La description est obligatoire")
        String description,

        @Positive(message = "Le temps de cuisson doit être positif")
        Integer tempsCuissonMinutes
) {
}
