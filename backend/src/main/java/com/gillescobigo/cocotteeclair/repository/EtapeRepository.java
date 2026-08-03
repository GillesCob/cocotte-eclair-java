package com.gillescobigo.cocotteeclair.repository;

import com.gillescobigo.cocotteeclair.entity.Etape;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EtapeRepository extends JpaRepository<Etape, UUID> {
    List<Etape> findByRecetteIdOrderByOrdreAsc(UUID recetteId);

    void deleteByRecetteId(UUID recetteId);
}
