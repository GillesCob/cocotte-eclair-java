package com.gillescobigo.cocotteeclair.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recettes")
public class Recette {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Nullable : les recettes de base système (compte CocotteEclair) n'ont pas de propriétaire personnel.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietaire_id")
    private User proprietaire;

    // Nullable : posé dès la V1 pour ne pas casser le modèle quand copie/partage seront construits,
    // même si la fonctionnalité de partage elle-même est différée.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recette_parente_id")
    private Recette recetteParente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibilite visibilite = Visibilite.PRIVEE;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Recette() {
    }

    public Recette(String titre, String description, User proprietaire) {
        this.id = UUID.randomUUID();
        this.titre = titre;
        this.description = description;
        this.proprietaire = proprietaire;
    }

    public UUID getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getProprietaire() {
        return proprietaire;
    }

    public Recette getRecetteParente() {
        return recetteParente;
    }

    public void setRecetteParente(Recette recetteParente) {
        this.recetteParente = recetteParente;
    }

    public Visibilite getVisibilite() {
        return visibilite;
    }

    public void setVisibilite(Visibilite visibilite) {
        this.visibilite = visibilite;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public enum Visibilite {
        PRIVEE, PUBLIQUE
    }
}
