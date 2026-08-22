package com.cdasanpedro.infrastructure.persistence.repository;

import com.cdasanpedro.infrastructure.persistence.entity.OrdenIngresoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrdenIngresoRepository extends JpaRepository<OrdenIngresoEntity, UUID> {
    Optional<OrdenIngresoEntity> findByConsecutivo(Long consecutivo);

    @Query("SELECT o FROM OrdenIngresoEntity o WHERE o.fechaIngreso BETWEEN :start AND :end ORDER BY o.fechaIngreso DESC")
    List<OrdenIngresoEntity> findByFechaIngresoBetween(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @Query("SELECT COUNT(o) FROM OrdenIngresoEntity o WHERE o.fechaIngreso BETWEEN :start AND :end")
    long countIngresosDia(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);
}
