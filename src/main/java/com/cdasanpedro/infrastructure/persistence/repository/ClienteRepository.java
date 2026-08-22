package com.cdasanpedro.infrastructure.persistence.repository;

import com.cdasanpedro.infrastructure.persistence.entity.ClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClienteRepository extends JpaRepository<ClienteEntity, UUID> {
    Optional<ClienteEntity> findByNumeroDocumento(String numeroDocumento);
    boolean existsByNumeroDocumento(String numeroDocumento);

    @Query("SELECT c FROM ClienteEntity c WHERE LOWER(c.nombresRazonSocial) LIKE LOWER(CONCAT('%', :query, '%')) OR c.numeroDocumento LIKE CONCAT('%', :query, '%')")
    List<ClienteEntity> searchClientes(@Param("query") String query);

    @Query("SELECT c FROM ClienteEntity c WHERE MONTH(c.fechaNacimiento) = :mes AND DAY(c.fechaNacimiento) = :dia")
    List<ClienteEntity> findClientesCumpleanos(@Param("mes") int mes, @Param("dia") int dia);
}
