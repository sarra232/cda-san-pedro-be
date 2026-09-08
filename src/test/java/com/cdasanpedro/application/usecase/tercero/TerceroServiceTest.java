package com.cdasanpedro.application.usecase.tercero;

import com.cdasanpedro.application.dto.tercero.TerceroRequestDto;
import com.cdasanpedro.application.dto.tercero.TerceroResponseDto;
import com.cdasanpedro.core.model.enums.TipoDocumento;
import com.cdasanpedro.core.model.enums.TipoPersona;
import com.cdasanpedro.core.model.enums.TipoRolTercero;
import com.cdasanpedro.infrastructure.persistence.entity.TerceroEntity;
import com.cdasanpedro.infrastructure.persistence.repository.TerceroRepository;
import com.cdasanpedro.infrastructure.persistence.repository.TerceroRolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TerceroServiceTest {

    @Mock
    private TerceroRepository repository;

    @Mock
    private TerceroRolRepository rolRepository;

    @InjectMocks
    private TerceroService service;

    private TerceroEntity entity;

    @BeforeEach
    void setUp() {
        entity = TerceroEntity.builder()
                .id(UUID.randomUUID())
                .tipoDocumento(TipoDocumento.NIT)
                .numeroDocumento("900999888")
                .tipoPersona(TipoPersona.JURIDICA)
                .razonSocialONombre("Proveedor Equipos S.A.S.")
                .celularPrincipal("3001234567")
                .activo(true)
                .roles(new ArrayList<>())
                .build();
    }

    @Test
    void testCrearTerceroExitoso() {
        TerceroRequestDto request = TerceroRequestDto.builder()
                .tipoDocumento(TipoDocumento.NIT)
                .numeroDocumento("900999888")
                .tipoPersona(TipoPersona.JURIDICA)
                .razonSocialONombre("Proveedor Equipos S.A.S.")
                .celularPrincipal("3001234567")
                .roles(Collections.singletonList(TipoRolTercero.PROVEEDOR))
                .build();

        when(repository.existsByNumeroDocumento("900999888")).thenReturn(false);
        when(repository.save(any(TerceroEntity.class))).thenReturn(entity);

        TerceroResponseDto result = service.crear(request);

        assertNotNull(result);
        assertEquals("900999888", result.getNumeroDocumento());
        assertEquals("Proveedor Equipos S.A.S.", result.getRazonSocialONombre());
    }

    @Test
    void testCrearTerceroDuplicadoLanzaExcepcion() {
        TerceroRequestDto request = TerceroRequestDto.builder()
                .numeroDocumento("900999888")
                .build();

        when(repository.existsByNumeroDocumento("900999888")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.crear(request));
    }
}
