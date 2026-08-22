package com.cdasanpedro.infrastructure.persistence.repository;

import com.cdasanpedro.core.model.enums.CategoriaVehiculo;
import com.cdasanpedro.infrastructure.persistence.entity.TarifaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TarifaRepository extends JpaRepository<TarifaEntity, UUID> {

    Optional<TarifaEntity> findByCodigo(String codigo);

    Optional<TarifaEntity> findFirstByCategoriaAndTipoServicioAndActivoTrue(CategoriaVehiculo categoria, String tipoServicio);

    Optional<TarifaEntity> findFirstByCategoriaAndActivoTrueOrderByCreatedAtDesc(CategoriaVehiculo categoria);

    List<TarifaEntity> findByCategoria(CategoriaVehiculo categoria);

    List<TarifaEntity> findByActivoTrueOrderByCategoriaAsc();

    List<TarifaEntity> findAllByOrderByCategoriaAscNombreServicioAsc();
}
