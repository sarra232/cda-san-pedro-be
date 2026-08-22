package com.cdasanpedro.application.usecase.tarifa;

import com.cdasanpedro.application.dto.tarifa.TarifaCreateRequestDto;
import com.cdasanpedro.application.dto.tarifa.TarifaResponseDto;
import com.cdasanpedro.application.dto.tarifa.TarifaUpdateRequestDto;
import com.cdasanpedro.core.exception.BusinessException;
import com.cdasanpedro.core.exception.ResourceNotFoundException;
import com.cdasanpedro.core.model.enums.CategoriaVehiculo;
import com.cdasanpedro.core.model.enums.RolUsuario;
import com.cdasanpedro.infrastructure.persistence.entity.TarifaEntity;
import com.cdasanpedro.infrastructure.persistence.entity.UsuarioEntity;
import com.cdasanpedro.infrastructure.persistence.repository.TarifaRepository;
import com.cdasanpedro.infrastructure.persistence.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TarifaService {

    private final TarifaRepository tarifaRepository;
    private final UsuarioRepository usuarioRepository;

    private static final BigDecimal DEFAULT_LIVIANO = new BigDecimal("320000.00");
    private static final BigDecimal DEFAULT_MOTO = new BigDecimal("210000.00");
    private static final BigDecimal DEFAULT_PESADO = new BigDecimal("450000.00");
    private static final BigDecimal DEFAULT_PUBLICO = new BigDecimal("350000.00");

    @Transactional(readOnly = true)
    public List<TarifaResponseDto> listarTarifas(CategoriaVehiculo categoria, Boolean soloActivos) {
        List<TarifaEntity> list;
        if (categoria != null) {
            list = tarifaRepository.findByCategoria(categoria);
        } else if (Boolean.TRUE.equals(soloActivos)) {
            list = tarifaRepository.findByActivoTrueOrderByCategoriaAsc();
        } else {
            list = tarifaRepository.findAllByOrderByCategoriaAscNombreServicioAsc();
        }

        if (Boolean.TRUE.equals(soloActivos) && categoria != null) {
            list = list.stream().filter(TarifaEntity::getActivo).collect(Collectors.toList());
        }

        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TarifaResponseDto obtenerPorId(UUID id) {
        TarifaEntity entity = tarifaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarifa/Servicio no encontrado con ID: " + id));
        return toDto(entity);
    }

    @Transactional(readOnly = true)
    public BigDecimal obtenerPrecioPorCategoria(CategoriaVehiculo categoria) {
        if (categoria == null) return DEFAULT_LIVIANO;

        return tarifaRepository.findFirstByCategoriaAndTipoServicioAndActivoTrue(categoria, "RTM_LEGAL")
                .or(() -> tarifaRepository.findFirstByCategoriaAndActivoTrueOrderByCreatedAtDesc(categoria))
                .map(TarifaEntity::getPrecio)
                .orElseGet(() -> switch (categoria) {
                    case MOTO -> DEFAULT_MOTO;
                    case PESADO -> DEFAULT_PESADO;
                    case PUBLICO -> DEFAULT_PUBLICO;
                    default -> DEFAULT_LIVIANO;
                });
    }

    @Transactional
    public TarifaResponseDto crearTarifa(TarifaCreateRequestDto request, UUID usuarioId) {
        validarPermisoAdmin(usuarioId);

        String codigo = request.getCodigo();
        if (codigo == null || codigo.isBlank()) {
            codigo = "SRV-" + request.getCategoria() + "-" + System.currentTimeMillis() % 10000;
        }

        TarifaEntity entity = TarifaEntity.builder()
                .codigo(codigo.trim().toUpperCase())
                .categoria(request.getCategoria())
                .tipoServicio(request.getTipoServicio() != null && !request.getTipoServicio().isBlank() 
                        ? request.getTipoServicio().trim().toUpperCase() 
                        : "RTM_LEGAL")
                .nombreServicio(request.getNombreServicio().trim())
                .descripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : null)
                .precio(request.getPrecio())
                .ivaPorcentaje(request.getIvaPorcentaje() != null ? request.getIvaPorcentaje() : BigDecimal.ZERO)
                .activo(request.getActivo() != null ? request.getActivo() : true)
                .build();

        TarifaEntity guardada = tarifaRepository.save(entity);
        return toDto(guardada);
    }

    @Transactional
    public TarifaResponseDto actualizarTarifa(UUID id, TarifaUpdateRequestDto request, UUID usuarioId) {
        validarPermisoAdmin(usuarioId);

        TarifaEntity entity = tarifaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarifa/Servicio no encontrado con ID: " + id));

        if (request.getCodigo() != null && !request.getCodigo().isBlank()) {
            entity.setCodigo(request.getCodigo().trim().toUpperCase());
        }
        if (request.getCategoria() != null) {
            entity.setCategoria(request.getCategoria());
        }
        if (request.getTipoServicio() != null && !request.getTipoServicio().isBlank()) {
            entity.setTipoServicio(request.getTipoServicio().trim().toUpperCase());
        }
        entity.setNombreServicio(request.getNombreServicio().trim());
        if (request.getDescripcion() != null) {
            entity.setDescripcion(request.getDescripcion().trim());
        }
        entity.setPrecio(request.getPrecio());
        if (request.getIvaPorcentaje() != null) {
            entity.setIvaPorcentaje(request.getIvaPorcentaje());
        }
        if (request.getActivo() != null) {
            entity.setActivo(request.getActivo());
        }

        TarifaEntity guardada = tarifaRepository.save(entity);
        return toDto(guardada);
    }

    @Transactional
    public void eliminarTarifa(UUID id, UUID usuarioId) {
        validarPermisoAdmin(usuarioId);

        TarifaEntity entity = tarifaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarifa/Servicio no encontrado con ID: " + id));

        tarifaRepository.delete(entity);
    }

    private void validarPermisoAdmin(UUID usuarioId) {
        if (usuarioId != null) {
            UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));

            if (usuario.getRol() != RolUsuario.ADMINISTRADOR) {
                throw new BusinessException("Solo los administradores tienen autorización para gestionar o modificar el catálogo de servicios y tarifas oficiales.");
            }
        }
    }

    public TarifaResponseDto toDto(TarifaEntity entity) {
        return TarifaResponseDto.builder()
                .id(entity.getId())
                .codigo(entity.getCodigo())
                .categoria(entity.getCategoria())
                .tipoServicio(entity.getTipoServicio())
                .nombreServicio(entity.getNombreServicio())
                .descripcion(entity.getDescripcion())
                .precio(entity.getPrecio())
                .ivaPorcentaje(entity.getIvaPorcentaje())
                .activo(entity.getActivo())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
