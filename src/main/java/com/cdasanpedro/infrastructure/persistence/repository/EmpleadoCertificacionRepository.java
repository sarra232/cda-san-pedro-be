package com.cdasanpedro.infrastructure.persistence.repository;

import com.cdasanpedro.infrastructure.persistence.entity.EmpleadoCertificacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmpleadoCertificacionRepository extends JpaRepository<EmpleadoCertificacionEntity, UUID> {

    List<EmpleadoCertificacionEntity> findByEmpleadoIdOrderByFechaVencimientoAsc(UUID empleadoId);

    @Query("SELECT c FROM EmpleadoCertificacionEntity c JOIN FETCH c.empleado e JOIN FETCH e.tercero ORDER BY c.fechaVencimiento ASC")
    List<EmpleadoCertificacionEntity> findAllWithEmpleado();

    @Query("SELECT c FROM EmpleadoCertificacionEntity c JOIN FETCH c.empleado e JOIN FETCH e.tercero WHERE c.fechaVencimiento <= :fechaLimite AND c.estado = 'VIGENTE'")
    List<EmpleadoCertificacionEntity> findProximasAVencer(LocalDate fechaLimite);
}
