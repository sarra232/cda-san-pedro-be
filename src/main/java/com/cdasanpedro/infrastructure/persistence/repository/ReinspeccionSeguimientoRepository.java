package com.cdasanpedro.infrastructure.persistence.repository;

import com.cdasanpedro.core.model.enums.EstadoReinspeccion;
import com.cdasanpedro.infrastructure.persistence.entity.ReinspeccionSeguimientoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReinspeccionSeguimientoRepository extends JpaRepository<ReinspeccionSeguimientoEntity, UUID> {

    Optional<ReinspeccionSeguimientoEntity> findByOrdenRechazadaId(UUID ordenRechazadaId);

    @Query("SELECT r FROM ReinspeccionSeguimientoEntity r WHERE r.vehiculo.placa = :placa AND r.reinspeccionCompletada = false ORDER BY r.fechaRechazo DESC")
    List<ReinspeccionSeguimientoEntity> findPendientesPorPlaca(@Param("placa") String placa);

    List<ReinspeccionSeguimientoEntity> findByEstadoSeguimientoAndReinspeccionCompletadaFalse(EstadoReinspeccion estado);

    @Query("SELECT r FROM ReinspeccionSeguimientoEntity r WHERE r.fechaLimite15Dias < :ahora AND r.reinspeccionCompletada = false AND r.estadoSeguimiento = 'EN_PLAZO'")
    List<ReinspeccionSeguimientoEntity> findVencidosSinReingresar(@Param("ahora") OffsetDateTime ahora);
}
