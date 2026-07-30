package com.gillescobigo.cocotteeclair.dto;

import com.gillescobigo.cocotteeclair.entity.Etape;

import java.util.UUID;

public record EtapeResponse(UUID id, Integer ordre, String description, Integer tempsCuissonMinutes) {
    public static EtapeResponse from(Etape etape) {
        return new EtapeResponse(etape.getId(), etape.getOrdre(), etape.getDescription(), etape.getTempsCuissonMinutes());
    }
}
