package com.cdasanpedro.infrastructure.persistence.repository;

import com.cdasanpedro.infrastructure.persistence.entity.PagoProveedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PagoProveedorRepository extends JpaRepository<PagoProveedorEntity, UUID> {

    List<PagoProveedorEntity> findByCuentaPorPagarIdOrderByFechaPagoDesc(UUID cuentaPorPagarId);
}
