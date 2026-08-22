package com.cdasanpedro.infrastructure.persistence.repository;

import com.cdasanpedro.core.model.enums.TipoPrueba;
import com.cdasanpedro.infrastructure.persistence.entity.PruebaInspeccionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PruebaInspeccionRepository extends JpaRepository<PruebaInspeccionEntity, UUID> {
    List<PruebaInspeccionEntity> findByOrdenIngresoIdOrderByCreatedAtAsc(UUID ordenIngresoId);
    Optional<PruebaInspeccionEntity> findByOrdenIngresoIdAndTipoPrueba(UUID ordenIngresoId, TipoPrueba tipoPrueba);
}
