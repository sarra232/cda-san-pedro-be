package com.cdasanpedro.application.usecase.cliente;

import com.cdasanpedro.application.dto.cliente.ClienteRequestDto;
import com.cdasanpedro.application.dto.cliente.ClienteResponseDto;
import com.cdasanpedro.core.model.enums.TipoDocumento;
import com.cdasanpedro.infrastructure.persistence.entity.ClienteEntity;
import com.cdasanpedro.infrastructure.persistence.repository.ClienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    @Test
    @DisplayName("Debe registrar un nuevo cliente con datos válidos")
    void registrarCliente_Nuevo() {
        ClienteRequestDto request = ClienteRequestDto.builder()
                .tipoDocumento(TipoDocumento.CC)
                .numeroDocumento("1020304050")
                .nombresRazonSocial("Juan Perez Gomez")
                .celular("3001234567")
                .email("juan@test.com")
                .fechaNacimiento(LocalDate.of(1990, 5, 15))
                .build();

        ClienteEntity entitySaved = ClienteEntity.builder()
                .id(UUID.randomUUID())
                .tipoDocumento(request.getTipoDocumento())
                .numeroDocumento(request.getNumeroDocumento())
                .nombresRazonSocial(request.getNombresRazonSocial())
                .celular(request.getCelular())
                .email(request.getEmail())
                .fechaNacimiento(request.getFechaNacimiento())
                .build();

        when(clienteRepository.findByNumeroDocumento("1020304050")).thenReturn(Optional.empty());
        when(clienteRepository.save(any(ClienteEntity.class))).thenReturn(entitySaved);

        ClienteResponseDto result = clienteService.registrarOActualizar(request);

        assertNotNull(result);
        assertEquals("1020304050", result.getNumeroDocumento());
        assertEquals("Juan Perez Gomez", result.getNombresRazonSocial());
        verify(clienteRepository, times(1)).save(any(ClienteEntity.class));
    }
}
