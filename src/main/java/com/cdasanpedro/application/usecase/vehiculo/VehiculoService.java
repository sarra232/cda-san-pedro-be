package com.cdasanpedro.application.usecase.vehiculo;

import com.cdasanpedro.application.dto.cliente.ClienteResponseDto;
import com.cdasanpedro.application.dto.vehiculo.VehiculoRequestDto;
import com.cdasanpedro.application.dto.vehiculo.VehiculoResponseDto;
import com.cdasanpedro.application.usecase.cliente.ClienteService;
import com.cdasanpedro.core.exception.ResourceNotFoundException;
import com.cdasanpedro.infrastructure.persistence.entity.ClienteEntity;
import com.cdasanpedro.infrastructure.persistence.entity.VehiculoEntity;
import com.cdasanpedro.infrastructure.persistence.repository.ClienteRepository;
import com.cdasanpedro.infrastructure.persistence.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehiculoService {

    private final VehiculoRepository vehiculoRepository;
    private final ClienteRepository clienteRepository;
    private final ClienteService clienteService;

    @Transactional
    public VehiculoResponseDto registrarOActualizar(VehiculoRequestDto request) {
        String placaLimpia = request.getPlaca().replaceAll("[^A-Za-z0-9]", "").toUpperCase();

        VehiculoEntity vehiculo = vehiculoRepository.findByPlaca(placaLimpia)
                .orElseGet(() -> VehiculoEntity.builder()
                        .placa(placaLimpia)
                        .build());

        vehiculo.setCategoria(request.getCategoria());
        vehiculo.setMarca(request.getMarca().trim().toUpperCase());
        vehiculo.setLinea(request.getLinea().trim().toUpperCase());
        vehiculo.setModelo(request.getModelo());
        vehiculo.setChasisVin(request.getChasisVin() != null ? request.getChasisVin().trim().toUpperCase() : null);
        vehiculo.setFechaVencimientoSoat(request.getFechaVencimientoSoat());
        vehiculo.setFechaVencimientoRtm(request.getFechaVencimientoRtm());

        if (request.getPropietarioId() != null) {
            ClienteEntity propietario = clienteRepository.findById(request.getPropietarioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Propietario no encontrado con ID: " + request.getPropietarioId()));
            vehiculo.setPropietario(propietario);
        }

        VehiculoEntity saved = vehiculoRepository.save(vehiculo);
        ClienteResponseDto propDto = saved.getPropietario() != null ? clienteService.toDto(saved.getPropietario()) : null;
        return VehiculoResponseDto.fromEntity(saved, propDto);
    }

    @Transactional(readOnly = true)
    public VehiculoResponseDto obtenerPorId(UUID id) {
        VehiculoEntity entity = vehiculoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehículo no encontrado con ID: " + id));
        ClienteResponseDto propDto = entity.getPropietario() != null ? clienteService.toDto(entity.getPropietario()) : null;
        return VehiculoResponseDto.fromEntity(entity, propDto);
    }

    @Transactional(readOnly = true)
    public Optional<VehiculoResponseDto> buscarPorPlaca(String placa) {
        String placaLimpia = placa.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        return vehiculoRepository.findByPlaca(placaLimpia)
                .map(v -> {
                    ClienteResponseDto propDto = v.getPropietario() != null ? clienteService.toDto(v.getPropietario()) : null;
                    return VehiculoResponseDto.fromEntity(v, propDto);
                });
    }

    @Transactional(readOnly = true)
    public List<VehiculoResponseDto> listarTodos() {
        return vehiculoRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public VehiculoResponseDto toDto(VehiculoEntity entity) {
        if (entity == null) return null;
        ClienteResponseDto propDto = entity.getPropietario() != null ? clienteService.toDto(entity.getPropietario()) : null;
        return VehiculoResponseDto.fromEntity(entity, propDto);
    }
}
