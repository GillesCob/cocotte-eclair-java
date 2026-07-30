package com.gillescobigo.cocotteeclair.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "ingredients")
public class Ingredient {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String nom;

    protected Ingredient() {
    }

    public Ingredient(String nom) {
        this.id = UUID.randomUUID();
        this.nom = nom;
    }

    public UUID getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }
}
