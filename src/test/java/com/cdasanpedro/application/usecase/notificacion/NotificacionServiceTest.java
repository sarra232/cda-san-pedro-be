package com.cdasanpedro.application.usecase.notificacion;

import com.cdasanpedro.core.gateway.NotificationGateway;
import com.cdasanpedro.infrastructure.persistence.entity.ClienteEntity;
import com.cdasanpedro.infrastructure.persistence.entity.NotificacionEntity;
import com.cdasanpedro.infrastructure.persistence.entity.VehiculoEntity;
import com.cdasanpedro.infrastructure.persistence.repository.ClienteRepository;
import com.cdasanpedro.infrastructure.persistence.repository.NotificacionRepository;
import com.cdasanpedro.infrastructure.persistence.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private VehiculoRepository vehiculoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private NotificationGateway notificationGateway;

    @InjectMocks
    private NotificacionService notificacionService;

    private ClienteEntity clienteMock;
    private VehiculoEntity vehiculoMock;

    @BeforeEach
    void setUp() {
        clienteMock = ClienteEntity.builder()
                .id(UUID.randomUUID())
                .nombresRazonSocial("Cliente Cumpleañero")
                .celular("3101234567")
                .fechaNacimiento(LocalDate.now())
                .build();

        vehiculoMock = VehiculoEntity.builder()
                .id(UUID.randomUUID())
                .placa("ABC123")
                .propietario(clienteMock)
                .fechaVencimientoSoat(LocalDate.now().plusDays(15))
                .build();
    }

    @Test
    @DisplayName("Debe ejecutar barrido diario y encolar recordatorio de SOAT a 15 días y cumpleaños")
    void procesarBarridoDiario() {
        when(vehiculoRepository.findAll()).thenReturn(List.of(vehiculoMock));
        when(clienteRepository.findAll()).thenReturn(List.of(clienteMock));
        when(notificacionRepository.findPendientesParaEnvio(any())).thenReturn(List.of());

        notificacionService.procesarBarridoDiario();

        // Debe haber encolado al menos 2 notificaciones (SOAT a 15 días + Cumpleaños de hoy)
        verify(notificacionRepository, atLeast(2)).save(any(NotificacionEntity.class));
    }
}
