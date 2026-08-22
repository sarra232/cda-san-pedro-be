package com.cdasanpedro.infrastructure.persistence.repository;

import com.cdasanpedro.infrastructure.persistence.entity.VehiculoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehiculoRepository extends JpaRepository<VehiculoEntity, UUID> {
    Optional<VehiculoEntity> findByPlaca(String placa);
    boolean existsByPlaca(String placa);

    @Query("SELECT v FROM VehiculoEntity v WHERE v.fechaVencimientoSoat BETWEEN :startDate AND :endDate")
    List<VehiculoEntity> findVehiculosSoatProximoVencer(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT v FROM VehiculoEntity v WHERE v.fechaVencimientoRtm BETWEEN :startDate AND :endDate")
    List<VehiculoEntity> findVehiculosRtmProximoVencer(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
