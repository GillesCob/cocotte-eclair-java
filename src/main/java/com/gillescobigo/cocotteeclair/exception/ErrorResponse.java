package com.gillescobigo.cocotteeclair.exception;

// Un seul nom de champ pour le message d'erreur API, quel que soit le type d'erreur (checklist V1 native).
public record ErrorResponse(String message) {
}
