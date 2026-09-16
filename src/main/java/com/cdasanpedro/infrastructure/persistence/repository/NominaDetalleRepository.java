package com.cdasanpedro.infrastructure.persistence.repository;

import com.cdasanpedro.infrastructure.persistence.entity.NominaDetalleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NominaDetalleRepository extends JpaRepository<NominaDetalleEntity, UUID> {
    List<NominaDetalleEntity> findByNominaId(UUID nominaId);
    List<NominaDetalleEntity> findByEmpleadoId(UUID empleadoId);
}
