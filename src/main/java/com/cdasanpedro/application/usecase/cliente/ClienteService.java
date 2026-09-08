package com.cdasanpedro.application.usecase.cliente;

import com.cdasanpedro.application.dto.cliente.ClienteRequestDto;
import com.cdasanpedro.application.dto.cliente.ClienteResponseDto;
import com.cdasanpedro.core.exception.ResourceNotFoundException;
import com.cdasanpedro.infrastructure.persistence.entity.ClienteEntity;
import com.cdasanpedro.infrastructure.persistence.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    @Transactional
    public ClienteResponseDto registrarOActualizar(ClienteRequestDto request) {
        String numDoc = request.getNumeroDocumento().trim();

        ClienteEntity cliente = clienteRepository.findByNumeroDocumento(numDoc)
                .orElseGet(() -> ClienteEntity.builder()
                        .numeroDocumento(numDoc)
                        .build());

        cliente.setTipoDocumento(request.getTipoDocumento());
        cliente.setNombresRazonSocial(request.getNombresRazonSocial().trim());
        cliente.setDireccion(request.getDireccion() != null ? request.getDireccion().trim() : null);
        cliente.setCelular(request.getCelular().trim());
        cliente.setEmail(request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null);
        cliente.setFechaNacimiento(request.getFechaNacimiento());

        ClienteEntity saved = clienteRepository.save(cliente);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public ClienteResponseDto obtenerPorId(UUID id) {
        ClienteEntity entity = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));
        return toDto(entity);
    }

    private final com.cdasanpedro.application.usecase.siigo.SiigoCustomerService siigoCustomerService;

    @Transactional
    public Optional<ClienteResponseDto> buscarPorDocumento(String numeroDocumento) {
        String cleanDoc = numeroDocumento.trim();
        Optional<ClienteEntity> local = clienteRepository.findByNumeroDocumento(cleanDoc);
        if (local.isPresent()) {
            return local.map(this::toDto);
        }

        // Búsqueda Just-in-Time en SIIGO Cloud API
        if (siigoCustomerService != null) {
            try {
                return siigoCustomerService.buscarYAutoguardarDesdeSiigo(cleanDoc).map(this::toDto);
            } catch (Exception ignored) {}
        }

        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public List<ClienteResponseDto> buscarClientes(String query) {
        if (query == null || query.trim().isEmpty()) {
            return listarTodos();
        }
        return clienteRepository.searchClientes(query.trim())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClienteResponseDto> listarTodos() {
        return clienteRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ClienteResponseDto toDto(ClienteEntity entity) {
        if (entity == null) return null;
        return ClienteResponseDto.builder()
                .id(entity.getId())
                .tipoDocumento(entity.getTipoDocumento())
                .numeroDocumento(entity.getNumeroDocumento())
                .nombresRazonSocial(entity.getNombresRazonSocial())
                .direccion(entity.getDireccion())
                .celular(entity.getCelular())
                .email(entity.getEmail())
                .fechaNacimiento(entity.getFechaNacimiento())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
