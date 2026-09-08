package com.cdasanpedro.infrastructure.persistence.repository;

import com.cdasanpedro.core.model.enums.EstadoFacturaDian;
import com.cdasanpedro.infrastructure.persistence.entity.FacturaElectronicaDianEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FacturaElectronicaDianRepository extends JpaRepository<FacturaElectronicaDianEntity, UUID> {

    Optional<FacturaElectronicaDianEntity> findByFacturaId(UUID facturaId);

    Optional<FacturaElectronicaDianEntity> findByCufe(String cufe);

    List<FacturaElectronicaDianEntity> findByEstadoDian(EstadoFacturaDian estadoDian);
}
