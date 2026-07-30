package com.gillescobigo.cocotteeclair.repository;

import com.gillescobigo.cocotteeclair.entity.RecetteIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecetteIngredientRepository extends JpaRepository<RecetteIngredient, UUID> {
    List<RecetteIngredient> findByRecetteId(UUID recetteId);
}
