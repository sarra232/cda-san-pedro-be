package com.cdasanpedro.application.usecase.vehiculo;

import com.cdasanpedro.application.dto.vehiculo.VehiculoRequestDto;
import com.cdasanpedro.application.dto.vehiculo.VehiculoResponseDto;
import com.cdasanpedro.application.usecase.cliente.ClienteService;
import com.cdasanpedro.core.model.enums.CategoriaVehiculo;
import com.cdasanpedro.infrastructure.persistence.entity.VehiculoEntity;
import com.cdasanpedro.infrastructure.persistence.repository.ClienteRepository;
import com.cdasanpedro.infrastructure.persistence.repository.VehiculoRepository;
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
class VehiculoServiceTest {

    @Mock
    private VehiculoRepository vehiculoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private VehiculoService vehiculoService;

    @Test
    @DisplayName("Debe limpiar la placa en mayúsculas y calcular alertas de vencimiento correctamente")
    void registrarVehiculo_CalculoAlertas() {
        LocalDate hoy = LocalDate.now();
        LocalDate soatVencido = hoy.minusDays(5);
        LocalDate rtmProximo = hoy.plusDays(10);

        VehiculoRequestDto request = VehiculoRequestDto.builder()
                .placa("abc-123")
                .categoria(CategoriaVehiculo.LIVIANO)
                .marca("CHEVROLET")
                .linea("SPARK")
                .modelo(2018)
                .fechaVencimientoSoat(soatVencido)
                .fechaVencimientoRtm(rtmProximo)
                .build();

        VehiculoEntity savedEntity = VehiculoEntity.builder()
                .id(UUID.randomUUID())
                .placa("ABC123")
                .categoria(CategoriaVehiculo.LIVIANO)
                .marca("CHEVROLET")
                .linea("SPARK")
                .modelo(2018)
                .fechaVencimientoSoat(soatVencido)
                .fechaVencimientoRtm(rtmProximo)
                .build();

        when(vehiculoRepository.findByPlaca("ABC123")).thenReturn(Optional.empty());
        when(vehiculoRepository.save(any(VehiculoEntity.class))).thenReturn(savedEntity);

        VehiculoResponseDto result = vehiculoService.registrarOActualizar(request);

        assertNotNull(result);
        assertEquals("ABC123", result.getPlaca());
        assertTrue(result.isSoatVencido(), "El SOAT debe marcarse como vencido");
        assertTrue(result.isRtmProximoVencer(), "La RTM debe marcarse como próxima a vencer");
        assertFalse(result.isRtmVencido(), "La RTM no ha vencido aún");
    }
}
