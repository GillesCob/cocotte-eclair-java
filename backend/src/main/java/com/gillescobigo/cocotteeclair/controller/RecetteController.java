package com.gillescobigo.cocotteeclair.controller;

import com.gillescobigo.cocotteeclair.dto.EtapeRequest;
import com.gillescobigo.cocotteeclair.dto.EtapeResponse;
import com.gillescobigo.cocotteeclair.dto.RecetteIngredientRequest;
import com.gillescobigo.cocotteeclair.dto.RecetteIngredientResponse;
import com.gillescobigo.cocotteeclair.dto.RecetteRequest;
import com.gillescobigo.cocotteeclair.dto.RecetteResponse;
import com.gillescobigo.cocotteeclair.entity.User;
import com.gillescobigo.cocotteeclair.service.RecetteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recettes")
public class RecetteController {

    private final RecetteService recetteService;

    public RecetteController(RecetteService recetteService) {
        this.recetteService = recetteService;
    }

    @GetMapping
    public List<RecetteResponse> findAll(@AuthenticationPrincipal User currentUser) {
        return recetteService.findVisibleFor(currentUser);
    }

    @GetMapping("/{id}")
    public RecetteResponse findById(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        return recetteService.findById(id, currentUser);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecetteResponse create(@Valid @RequestBody RecetteRequest request, @AuthenticationPrincipal User currentUser) {
        return recetteService.create(request, currentUser);
    }

    @PutMapping("/{id}")
    public RecetteResponse update(@PathVariable UUID id, @Valid @RequestBody RecetteRequest request, @AuthenticationPrincipal User currentUser) {
        return recetteService.update(id, request, currentUser);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        recetteService.delete(id, currentUser);
    }

    @PostMapping("/{id}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    public RecetteResponse copy(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        return recetteService.copy(id, currentUser);
    }

    @PostMapping("/{id}/ingredients")
    @ResponseStatus(HttpStatus.CREATED)
    public RecetteIngredientResponse addIngredient(
            @PathVariable UUID id,
            @Valid @RequestBody RecetteIngredientRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return recetteService.addIngredient(id, request, currentUser);
    }

    @DeleteMapping("/{id}/ingredients/{ingredientLineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeIngredient(
            @PathVariable UUID id,
            @PathVariable UUID ingredientLineId,
            @AuthenticationPrincipal User currentUser
    ) {
        recetteService.removeIngredient(id, ingredientLineId, currentUser);
    }

    @PutMapping("/{id}/ingredients/{ingredientLineId}")
    public RecetteIngredientResponse updateIngredient(
            @PathVariable UUID id,
            @PathVariable UUID ingredientLineId,
            @Valid @RequestBody RecetteIngredientRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return recetteService.updateIngredient(id, ingredientLineId, request, currentUser);
    }

    @PostMapping("/{id}/etapes")
    @ResponseStatus(HttpStatus.CREATED)
    public EtapeResponse addEtape(
            @PathVariable UUID id,
            @Valid @RequestBody EtapeRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return recetteService.addEtape(id, request, currentUser);
    }

    @DeleteMapping("/{id}/etapes/{etapeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeEtape(
            @PathVariable UUID id,
            @PathVariable UUID etapeId,
            @AuthenticationPrincipal User currentUser
    ) {
        recetteService.removeEtape(id, etapeId, currentUser);
    }

    @PutMapping("/{id}/etapes/{etapeId}")
    public EtapeResponse updateEtape(
            @PathVariable UUID id,
            @PathVariable UUID etapeId,
            @Valid @RequestBody EtapeRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return recetteService.updateEtape(id, etapeId, request, currentUser);
    }
}
