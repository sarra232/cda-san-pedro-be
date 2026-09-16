package com.cdasanpedro.infrastructure.persistence.repository;

import com.cdasanpedro.core.model.enums.TipoRolTercero;
import com.cdasanpedro.infrastructure.persistence.entity.TerceroEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TerceroRepository extends JpaRepository<TerceroEntity, UUID> {

    Optional<TerceroEntity> findByNumeroDocumento(String numeroDocumento);

    Optional<TerceroEntity> findByEmailPrincipal(String emailPrincipal);

    boolean existsByNumeroDocumento(String numeroDocumento);

    @Query("SELECT t FROM TerceroEntity t JOIN t.roles r WHERE r.tipoRol = :tipoRol AND t.activo = true AND r.activo = true ORDER BY t.razonSocialONombre ASC")
    List<TerceroEntity> findByTipoRol(@Param("tipoRol") TipoRolTercero tipoRol);

    @Query("SELECT t FROM TerceroEntity t WHERE " +
            "LOWER(t.numeroDocumento) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(t.razonSocialONombre) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(t.celularPrincipal) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<TerceroEntity> searchTerceros(@Param("query") String query);
}
