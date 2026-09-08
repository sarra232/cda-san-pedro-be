package com.cdasanpedro.infrastructure.persistence.repository;

import com.cdasanpedro.infrastructure.persistence.entity.MapeoCatalogoSiigoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MapeoCatalogoSiigoRepository extends JpaRepository<MapeoCatalogoSiigoEntity, UUID> {

    Optional<MapeoCatalogoSiigoEntity> findByCategoriaCdaAndActivoTrue(String categoriaCda);
}
