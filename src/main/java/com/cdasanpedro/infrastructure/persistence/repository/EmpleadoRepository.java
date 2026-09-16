package com.cdasanpedro.infrastructure.persistence.repository;

import com.cdasanpedro.core.model.enums.EstadoEmpleado;
import com.cdasanpedro.infrastructure.persistence.entity.EmpleadoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmpleadoRepository extends JpaRepository<EmpleadoEntity, UUID> {

    @Query("SELECT e FROM EmpleadoEntity e JOIN FETCH e.tercero WHERE e.estado = :estado ORDER BY e.cargo ASC")
    List<EmpleadoEntity> findByEstado(EstadoEmpleado estado);

    @Query("SELECT e FROM EmpleadoEntity e JOIN FETCH e.tercero ORDER BY e.cargo ASC")
    List<EmpleadoEntity> findAllWithTercero();

    Optional<EmpleadoEntity> findByTerceroId(UUID terceroId);

    @Query("SELECT e FROM EmpleadoEntity e JOIN FETCH e.tercero WHERE e.tercero.numeroDocumento = :documento")
    Optional<EmpleadoEntity> findByNumeroDocumento(String documento);
}
