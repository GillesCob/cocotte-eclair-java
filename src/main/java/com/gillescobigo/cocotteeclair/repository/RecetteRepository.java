package com.gillescobigo.cocotteeclair.repository;

import com.gillescobigo.cocotteeclair.entity.Recette;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecetteRepository extends JpaRepository<Recette, UUID> {
    List<Recette> findByProprietaireId(UUID proprietaireId);

    List<Recette> findByProprietaireIsNull();
}
