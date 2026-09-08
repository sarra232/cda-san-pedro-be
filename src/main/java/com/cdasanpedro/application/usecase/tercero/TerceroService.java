package com.cdasanpedro.application.usecase.tercero;

import com.cdasanpedro.application.dto.tercero.TerceroRequestDto;
import com.cdasanpedro.application.dto.tercero.TerceroResponseDto;
import com.cdasanpedro.application.dto.tercero.TerceroRolDto;
import com.cdasanpedro.core.model.enums.TipoRolTercero;
import com.cdasanpedro.infrastructure.persistence.entity.TerceroEntity;
import com.cdasanpedro.infrastructure.persistence.entity.TerceroRolEntity;
import com.cdasanpedro.infrastructure.persistence.repository.TerceroRepository;
import com.cdasanpedro.infrastructure.persistence.repository.TerceroRolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TerceroService {

    private final TerceroRepository terceroRepository;
    private final TerceroRolRepository terceroRolRepository;

    @Transactional(readOnly = true)
    public List<TerceroResponseDto> listarTodos() {
        return terceroRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TerceroResponseDto> listarPorRol(TipoRolTercero tipoRol) {
        return terceroRepository.findByTipoRol(tipoRol).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TerceroResponseDto obtenerPorId(UUID id) {
        TerceroEntity entity = terceroRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tercero no encontrado con ID: " + id));
        return mapToResponse(entity);
    }

    @Transactional(readOnly = true)
    public TerceroResponseDto obtenerPorDocumento(String numeroDocumento) {
        TerceroEntity entity = terceroRepository.findByNumeroDocumento(numeroDocumento)
                .orElseThrow(() -> new IllegalArgumentException("Tercero no encontrado con documento: " + numeroDocumento));
        return mapToResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<TerceroResponseDto> buscar(String query) {
        if (query == null || query.isBlank()) {
            return listarTodos();
        }
        return terceroRepository.searchTerceros(query).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TerceroResponseDto crear(TerceroRequestDto request) {
        if (terceroRepository.existsByNumeroDocumento(request.getNumeroDocumento())) {
            throw new IllegalArgumentException("Ya existe un tercero registrado con el documento: " + request.getNumeroDocumento());
        }

        TerceroEntity entity = TerceroEntity.builder()
                .tipoDocumento(request.getTipoDocumento())
                .numeroDocumento(request.getNumeroDocumento().trim())
                .digitoVerificacion(request.getDigitoVerificacion())
                .tipoPersona(request.getTipoPersona())
                .razonSocialONombre(request.getRazonSocialONombre().trim())
                .primerNombre(request.getPrimerNombre())
                .otrosNombres(request.getOtrosNombres())
                .primerApellido(request.getPrimerApellido())
                .segundoApellido(request.getSegundoApellido())
                .celularPrincipal(request.getCelularPrincipal().trim())
                .telefonoSecundario(request.getTelefonoSecundario())
                .emailPrincipal(request.getEmailPrincipal())
                .emailFacturacion(request.getEmailFacturacion())
                .direccion(request.getDireccion())
                .municipioDane(request.getMunicipioDane())
                .departamentoDane(request.getDepartamentoDane())
                .responsabilidadFiscal(request.getResponsabilidadFiscal() != null ? request.getResponsabilidadFiscal() : "R-99-PN")
                .metadata(request.getMetadata() != null ? request.getMetadata() : "{}")
                .activo(request.getActivo() != null ? request.getActivo() : true)
                .roles(new ArrayList<>())
                .build();

        TerceroEntity guardado = terceroRepository.save(entity);

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            for (TipoRolTercero rol : request.getRoles()) {
                TerceroRolEntity rolEntity = TerceroRolEntity.builder()
                        .tercero(guardado)
                        .tipoRol(rol)
                        .metadataRol("{}")
                        .activo(true)
                        .build();
                guardado.getRoles().add(rolEntity);
            }
            guardado = terceroRepository.save(guardado);
        }

        return mapToResponse(guardado);
    }

    @Transactional
    public TerceroResponseDto actualizar(UUID id, TerceroRequestDto request) {
        TerceroEntity entity = terceroRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tercero no encontrado con ID: " + id));

        if (!entity.getNumeroDocumento().equals(request.getNumeroDocumento()) &&
                terceroRepository.existsByNumeroDocumento(request.getNumeroDocumento())) {
            throw new IllegalArgumentException("El número de documento ya está en uso por otro tercero");
        }

        entity.setTipoDocumento(request.getTipoDocumento());
        entity.setNumeroDocumento(request.getNumeroDocumento().trim());
        entity.setDigitoVerificacion(request.getDigitoVerificacion());
        if (request.getTipoPersona() != null) entity.setTipoPersona(request.getTipoPersona());
        entity.setRazonSocialONombre(request.getRazonSocialONombre().trim());
        entity.setPrimerNombre(request.getPrimerNombre());
        entity.setOtrosNombres(request.getOtrosNombres());
        entity.setPrimerApellido(request.getPrimerApellido());
        entity.setSegundoApellido(request.getSegundoApellido());
        entity.setCelularPrincipal(request.getCelularPrincipal().trim());
        entity.setTelefonoSecundario(request.getTelefonoSecundario());
        entity.setEmailPrincipal(request.getEmailPrincipal());
        entity.setEmailFacturacion(request.getEmailFacturacion());
        entity.setDireccion(request.getDireccion());
        entity.setMunicipioDane(request.getMunicipioDane());
        entity.setDepartamentoDane(request.getDepartamentoDane());
        if (request.getResponsabilidadFiscal() != null) entity.setResponsabilidadFiscal(request.getResponsabilidadFiscal());
        if (request.getMetadata() != null) entity.setMetadata(request.getMetadata());
        if (request.getActivo() != null) entity.setActivo(request.getActivo());

        if (request.getRoles() != null) {
            for (TipoRolTercero rol : request.getRoles()) {
                if (!terceroRolRepository.existsByTerceroIdAndTipoRol(entity.getId(), rol)) {
                    TerceroRolEntity nuevoRol = TerceroRolEntity.builder()
                            .tercero(entity)
                            .tipoRol(rol)
                            .metadataRol("{}")
                            .activo(true)
                            .build();
                    entity.getRoles().add(nuevoRol);
                }
            }
        }

        return mapToResponse(terceroRepository.save(entity));
    }

    public TerceroResponseDto mapToResponse(TerceroEntity entity) {
        List<TerceroRolDto> rolesDto = entity.getRoles() != null ?
                entity.getRoles().stream()
                        .map(r -> TerceroRolDto.builder()
                                .id(r.getId())
                                .tipoRol(r.getTipoRol())
                                .metadataRol(r.getMetadataRol())
                                .activo(r.getActivo())
                                .build())
                        .collect(Collectors.toList()) : new ArrayList<>();

        return TerceroResponseDto.builder()
                .id(entity.getId())
                .tipoDocumento(entity.getTipoDocumento())
                .numeroDocumento(entity.getNumeroDocumento())
                .digitoVerificacion(entity.getDigitoVerificacion())
                .tipoPersona(entity.getTipoPersona())
                .razonSocialONombre(entity.getRazonSocialONombre())
                .primerNombre(entity.getPrimerNombre())
                .otrosNombres(entity.getOtrosNombres())
                .primerApellido(entity.getPrimerApellido())
                .segundoApellido(entity.getSegundoApellido())
                .celularPrincipal(entity.getCelularPrincipal())
                .telefonoSecundario(entity.getTelefonoSecundario())
                .emailPrincipal(entity.getEmailPrincipal())
                .emailFacturacion(entity.getEmailFacturacion())
                .direccion(entity.getDireccion())
                .municipioDane(entity.getMunicipioDane())
                .departamentoDane(entity.getDepartamentoDane())
                .responsabilidadFiscal(entity.getResponsabilidadFiscal())
                .metadata(entity.getMetadata())
                .activo(entity.getActivo())
                .roles(rolesDto)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
