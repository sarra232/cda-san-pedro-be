package com.cdasanpedro.infrastructure.persistence.repository;

import com.cdasanpedro.infrastructure.persistence.entity.FacturaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FacturaRepository extends JpaRepository<FacturaEntity, UUID> {
    Optional<FacturaEntity> findByNumeroFactura(String numeroFactura);
    Optional<FacturaEntity> findByConsecutivo(Long consecutivo);
    Optional<FacturaEntity> findByOrdenIngresoId(UUID ordenIngresoId);

    @Query("SELECT f FROM FacturaEntity f WHERE f.fechaEmision BETWEEN :start AND :end ORDER BY f.fechaEmision DESC")
    List<FacturaEntity> findByFechaEmisionBetween(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @Query("SELECT COALESCE(SUM(f.total), 0) FROM FacturaEntity f WHERE f.fechaEmision BETWEEN :start AND :end AND f.estado = 'PAGADA'")
    BigDecimal sumTotalByFechaEmisionBetween(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);
}
