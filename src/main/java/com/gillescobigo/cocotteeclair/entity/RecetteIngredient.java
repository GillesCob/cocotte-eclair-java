package com.gillescobigo.cocotteeclair.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

import java.util.UUID;

@Entity
@Table(name = "recette_ingredients")
public class RecetteIngredient {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recette_id", nullable = false)
    private Recette recette;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(nullable = false)
    private Double quantite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Unite unite;

    protected RecetteIngredient() {
    }

    public RecetteIngredient(Recette recette, Ingredient ingredient, Double quantite, Unite unite) {
        this.id = UUID.randomUUID();
        this.recette = recette;
        this.ingredient = ingredient;
        this.quantite = quantite;
        this.unite = unite;
    }

    public UUID getId() {
        return id;
    }

    public Recette getRecette() {
        return recette;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public Double getQuantite() {
        return quantite;
    }

    public void setQuantite(Double quantite) {
        this.quantite = quantite;
    }

    public Unite getUnite() {
        return unite;
    }

    public void setUnite(Unite unite) {
        this.unite = unite;
    }

    public enum Unite {
        GRAMME, KILOGRAMME, MILLILITRE, LITRE, UNITE, CUILLERE_A_SOUPE, CUILLERE_A_CAFE, PINCEE
    }
}
