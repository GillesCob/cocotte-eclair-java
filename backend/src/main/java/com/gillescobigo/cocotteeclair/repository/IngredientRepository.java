package com.gillescobigo.cocotteeclair.repository;

import com.gillescobigo.cocotteeclair.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IngredientRepository extends JpaRepository<Ingredient, UUID> {
    Optional<Ingredient> findByNomIgnoreCase(String nom);
}
