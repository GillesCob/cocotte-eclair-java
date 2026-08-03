package com.gillescobigo.cocotteeclair.service;

import com.gillescobigo.cocotteeclair.dto.EtapeRequest;
import com.gillescobigo.cocotteeclair.dto.EtapeResponse;
import com.gillescobigo.cocotteeclair.dto.RecetteIngredientRequest;
import com.gillescobigo.cocotteeclair.dto.RecetteIngredientResponse;
import com.gillescobigo.cocotteeclair.dto.RecetteRequest;
import com.gillescobigo.cocotteeclair.dto.RecetteResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RecetteService {

    private final RecetteRepository recetteRepository;
    private final IngredientRepository ingredientRepository;
    private final RecetteIngredientRepository recetteIngredientRepository;
    private final EtapeRepository etapeRepository;

    public RecetteService(
            RecetteRepository recetteRepository,
            IngredientRepository ingredientRepository,
            RecetteIngredientRepository recetteIngredientRepository,
            EtapeRepository etapeRepository
    ) {
        this.recetteRepository = recetteRepository;
        this.ingredientRepository = ingredientRepository;
        this.recetteIngredientRepository = recetteIngredientRepository;
        this.etapeRepository = etapeRepository;
    }

    public List<RecetteResponse> findVisibleFor(User currentUser) {
        return recetteRepository.findAll().stream()
                .filter(r -> isVisibleTo(r, currentUser))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public RecetteResponse findById(UUID id, User currentUser) {
        Recette recette = getOwnedOrVisible(id, currentUser, false);
        return toResponse(recette);
    }

    @Transactional
    public RecetteResponse create(RecetteRequest request, User currentUser) {
        Recette recette = new Recette(request.titre(), request.description(), currentUser);
        recette.setVisibilite(request.visibilite());
        recetteRepository.save(recette);
        return toResponse(recette);
    }

    @Transactional
    public RecetteResponse update(UUID id, RecetteRequest request, User currentUser) {
        Recette recette = getOwnedOrVisible(id, currentUser, true);
        recette.setTitre(request.titre());
        recette.setDescription(request.description());
        recette.setVisibilite(request.visibilite());
        recetteRepository.save(recette);
        return toResponse(recette);
    }

    @Transactional
    public void delete(UUID id, User currentUser) {
        Recette recette = getOwnedOrVisible(id, currentUser, true);
        recetteRepository.delete(recette);
    }

    // Copie d'une recette de base système ou d'une recette publique dans sa propre base :
    // même mécanisme technique pour les deux cas, seul le déclencheur diffère (spec V1).
    @Transactional
    public RecetteResponse copy(UUID id, User currentUser) {
        Recette source = getOwnedOrVisible(id, currentUser, false);

        Recette copie = new Recette(source.getTitre(), source.getDescription(), currentUser);
        copie.setVisibilite(Recette.Visibilite.PRIVEE);
        copie.setRecetteParente(source);
        recetteRepository.save(copie);

        for (RecetteIngredient ri : recetteIngredientRepository.findByRecetteId(source.getId())) {
            recetteIngredientRepository.save(new RecetteIngredient(copie, ri.getIngredient(), ri.getQuantite(), ri.getUnite()));
        }
        for (Etape etape : etapeRepository.findByRecetteIdOrderByOrdreAsc(source.getId())) {
            etapeRepository.save(new Etape(copie, etape.getOrdre(), etape.getDescription(), etape.getTempsCuissonMinutes()));
        }

        return toResponse(copie);
    }

    @Transactional
    public RecetteIngredientResponse addIngredient(UUID recetteId, RecetteIngredientRequest request, User currentUser) {
        Recette recette = getOwnedOrVisible(recetteId, currentUser, true);

        Ingredient ingredient = ingredientRepository.findByNomIgnoreCase(request.ingredientNom())
                .orElseGet(() -> ingredientRepository.save(new Ingredient(request.ingredientNom())));

        RecetteIngredient recetteIngredient = new RecetteIngredient(recette, ingredient, request.quantite(), request.unite());
        recetteIngredientRepository.save(recetteIngredient);
        return RecetteIngredientResponse.from(recetteIngredient);
    }

    @Transactional
    public void removeIngredient(UUID recetteId, UUID recetteIngredientId, User currentUser) {
        getOwnedOrVisible(recetteId, currentUser, true);

        RecetteIngredient ri = recetteIngredientRepository.findById(recetteIngredientId)
                .orElseThrow(() -> new ResourceNotFoundException("Ingrédient de recette introuvable"));

        if (!ri.getRecette().getId().equals(recetteId)) {
            throw new ResourceNotFoundException("Ingrédient de recette introuvable");
        }

        recetteIngredientRepository.delete(ri);
    }

    @Transactional
    public RecetteIngredientResponse updateIngredient(UUID recetteId, UUID recetteIngredientId, RecetteIngredientRequest request, User currentUser) {
        getOwnedOrVisible(recetteId, currentUser, true);

        RecetteIngredient ri = recetteIngredientRepository.findById(recetteIngredientId)
                .orElseThrow(() -> new ResourceNotFoundException("Ingrédient de recette introuvable"));

        if (!ri.getRecette().getId().equals(recetteId)) {
            throw new ResourceNotFoundException("Ingrédient de recette introuvable");
        }

        Ingredient ingredient = ingredientRepository.findByNomIgnoreCase(request.ingredientNom())
                .orElseGet(() -> ingredientRepository.save(new Ingredient(request.ingredientNom())));

        ri.setIngredient(ingredient);
        ri.setQuantite(request.quantite());
        ri.setUnite(request.unite());
        recetteIngredientRepository.save(ri);
        return RecetteIngredientResponse.from(ri);
    }

    @Transactional
    public EtapeResponse addEtape(UUID recetteId, EtapeRequest request, User currentUser) {
        Recette recette = getOwnedOrVisible(recetteId, currentUser, true);
        Etape etape = new Etape(recette, request.ordre(), request.description(), request.tempsCuissonMinutes());
        etapeRepository.save(etape);
        return EtapeResponse.from(etape);
    }

    @Transactional
    public void removeEtape(UUID recetteId, UUID etapeId, User currentUser) {
        getOwnedOrVisible(recetteId, currentUser, true);

        Etape etape = etapeRepository.findById(etapeId)
                .orElseThrow(() -> new ResourceNotFoundException("Étape introuvable"));

        if (!etape.getRecette().getId().equals(recetteId)) {
            throw new ResourceNotFoundException("Étape introuvable");
        }

        etapeRepository.delete(etape);
    }

    @Transactional
    public EtapeResponse updateEtape(UUID recetteId, UUID etapeId, EtapeRequest request, User currentUser) {
        getOwnedOrVisible(recetteId, currentUser, true);

        Etape etape = etapeRepository.findById(etapeId)
                .orElseThrow(() -> new ResourceNotFoundException("Étape introuvable"));

        if (!etape.getRecette().getId().equals(recetteId)) {
            throw new ResourceNotFoundException("Étape introuvable");
        }

        etape.setOrdre(request.ordre());
        etape.setDescription(request.description());
        etape.setTempsCuissonMinutes(request.tempsCuissonMinutes());
        etapeRepository.save(etape);
        return EtapeResponse.from(etape);
    }

    // requireOwnership=true : opérations d'écriture, seul le propriétaire (jamais une recette
    // de base système, jamais une recette publique d'un autre utilisateur) peut agir.
    // requireOwnership=false : lecture, autorisée si privée-et-propriétaire, publique, ou recette de base.
    private Recette getOwnedOrVisible(UUID id, User currentUser, boolean requireOwnership) {
        Recette recette = recetteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recette introuvable"));

        boolean isOwner = recette.getProprietaire() != null && recette.getProprietaire().getId().equals(currentUser.getId());

        if (requireOwnership && !isOwner) {
            throw new UnauthorizedException("Vous n'êtes pas propriétaire de cette recette");
        }

        if (!requireOwnership && !isOwner && !isVisibleTo(recette, currentUser)) {
            throw new ResourceNotFoundException("Recette introuvable");
        }

        return recette;
    }

    private boolean isVisibleTo(Recette recette, User currentUser) {
        boolean isOwner = recette.getProprietaire() != null && recette.getProprietaire().getId().equals(currentUser.getId());
        boolean estRecetteDeBase = recette.getProprietaire() == null;
        boolean estPublique = recette.getVisibilite() == Recette.Visibilite.PUBLIQUE;
        return isOwner || estRecetteDeBase || estPublique;
    }

    private RecetteResponse toResponse(Recette recette) {
        List<RecetteIngredientResponse> ingredients = recetteIngredientRepository.findByRecetteId(recette.getId())
                .stream().map(RecetteIngredientResponse::from).collect(Collectors.toList());
        List<EtapeResponse> etapes = etapeRepository.findByRecetteIdOrderByOrdreAsc(recette.getId())
                .stream().map(EtapeResponse::from).collect(Collectors.toList());
        return RecetteResponse.from(recette, ingredients, etapes);
    }
}
