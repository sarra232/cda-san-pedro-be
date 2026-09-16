package com.cdasanpedro.infrastructure.persistence.repository;

import com.cdasanpedro.core.model.enums.TipoToken;
import com.cdasanpedro.infrastructure.persistence.entity.TokenAutenticacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenAutenticacionRepository extends JpaRepository<TokenAutenticacionEntity, UUID> {

    Optional<TokenAutenticacionEntity> findByToken(String token);

    @Query("SELECT t FROM TokenAutenticacionEntity t WHERE t.token = :token AND t.usado = false")
    Optional<TokenAutenticacionEntity> findByTokenAndUsadoFalse(@Param("token") String token);

    List<TokenAutenticacionEntity> findByUsuarioIdAndTipoAndUsadoFalse(UUID usuarioId, TipoToken tipo);
}
