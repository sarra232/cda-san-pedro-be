package com.cdasanpedro.infrastructure.persistence.repository;

import com.cdasanpedro.infrastructure.persistence.entity.ConfiguracionSistemaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConfiguracionSistemaRepository extends JpaRepository<ConfiguracionSistemaEntity, UUID> {
    Optional<ConfiguracionSistemaEntity> findByClave(String clave);
    List<ConfiguracionSistemaEntity> findByCategoria(String categoria);
}
