package com.cdasanpedro.infrastructure.persistence.repository;

import com.cdasanpedro.infrastructure.persistence.entity.NominaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NominaRepository extends JpaRepository<NominaEntity, UUID> {

    @Query("SELECT n FROM NominaEntity n LEFT JOIN FETCH n.detalles d LEFT JOIN FETCH d.empleado e LEFT JOIN FETCH e.tercero ORDER BY n.periodoAnio DESC, n.periodoMes DESC, n.periodoQuincena DESC")
    List<NominaEntity> findAllWithDetalles();

    Optional<NominaEntity> findByPeriodoAnioAndPeriodoMesAndPeriodoQuincena(Integer anio, Integer mes, Integer quincena);
}
