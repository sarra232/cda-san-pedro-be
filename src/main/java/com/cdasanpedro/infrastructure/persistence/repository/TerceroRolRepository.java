package com.cdasanpedro.infrastructure.persistence.repository;

import com.cdasanpedro.core.model.enums.TipoRolTercero;
import com.cdasanpedro.infrastructure.persistence.entity.TerceroRolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TerceroRolRepository extends JpaRepository<TerceroRolEntity, UUID> {

    List<TerceroRolEntity> findByTerceroId(UUID terceroId);

    Optional<TerceroRolEntity> findByTerceroIdAndTipoRol(UUID terceroId, TipoRolTercero tipoRol);

    boolean existsByTerceroIdAndTipoRol(UUID terceroId, TipoRolTercero tipoRol);
}
