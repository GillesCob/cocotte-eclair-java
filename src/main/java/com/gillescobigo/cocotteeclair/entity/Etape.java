package com.gillescobigo.cocotteeclair.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "etapes")
public class Etape {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recette_id", nullable = false)
    private Recette recette;

    @Column(nullable = false)
    private Integer ordre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    // Nullable : toutes les étapes n'ont pas de temps de cuisson (repos, préparation...).
    private Integer tempsCuissonMinutes;

    protected Etape() {
    }

    public Etape(Recette recette, Integer ordre, String description, Integer tempsCuissonMinutes) {
        this.id = UUID.randomUUID();
        this.recette = recette;
        this.ordre = ordre;
        this.description = description;
        this.tempsCuissonMinutes = tempsCuissonMinutes;
    }

    public UUID getId() {
        return id;
    }

    public Recette getRecette() {
        return recette;
    }

    public Integer getOrdre() {
        return ordre;
    }

    public void setOrdre(Integer ordre) {
        this.ordre = ordre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getTempsCuissonMinutes() {
        return tempsCuissonMinutes;
    }

    public void setTempsCuissonMinutes(Integer tempsCuissonMinutes) {
        this.tempsCuissonMinutes = tempsCuissonMinutes;
    }
}
