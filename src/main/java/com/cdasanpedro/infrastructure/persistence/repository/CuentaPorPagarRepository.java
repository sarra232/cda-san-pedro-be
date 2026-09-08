package com.cdasanpedro.infrastructure.persistence.repository;

import com.cdasanpedro.core.model.enums.EstadoCuentaPagar;
import com.cdasanpedro.infrastructure.persistence.entity.CuentaPorPagarEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface CuentaPorPagarRepository extends JpaRepository<CuentaPorPagarEntity, UUID> {

    List<CuentaPorPagarEntity> findByAcreedorId(UUID acreedorId);

    List<CuentaPorPagarEntity> findByEstado(EstadoCuentaPagar estado);

    @Query("SELECT c FROM CuentaPorPagarEntity c WHERE c.fechaVencimiento <= :fechaLimite AND c.estado IN ('PENDIENTE', 'PAGADA_PARCIAL') ORDER BY c.fechaVencimiento ASC")
    List<CuentaPorPagarEntity> findProximasAVencer(@Param("fechaLimite") LocalDate fechaLimite);

    @Query("SELECT c FROM CuentaPorPagarEntity c WHERE c.fechaVencimiento < :hoy AND c.estado IN ('PENDIENTE', 'PAGADA_PARCIAL') ORDER BY c.fechaVencimiento ASC")
    List<CuentaPorPagarEntity> findVencidas(@Param("hoy") LocalDate hoy);

    @Query("SELECT c FROM CuentaPorPagarEntity c ORDER BY c.fechaVencimiento ASC")
    List<CuentaPorPagarEntity> findAllOrderByVencimientoAsc();
}
