package com.cdasanpedro.infrastructure.persistence.repository;

import com.cdasanpedro.infrastructure.persistence.entity.NotificacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificacionRepository extends JpaRepository<NotificacionEntity, UUID> {
    
    @Query("SELECT n FROM NotificacionEntity n WHERE n.estado = 'PENDIENTE' AND n.fechaProgramada <= :now ORDER BY n.fechaProgramada ASC")
    List<NotificacionEntity> findPendientesParaEnvio(@Param("now") OffsetDateTime now);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE NotificacionEntity n SET n.estado = 'CANCELADO' WHERE n.cliente.id = :clienteId AND n.tipo = :tipo AND n.estado = 'PENDIENTE'")
    int cancelarPendientesPorClienteYTipo(@Param("clienteId") UUID clienteId, @Param("tipo") String tipo);
}
