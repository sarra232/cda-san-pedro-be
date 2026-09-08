package com.cdasanpedro.web.controller;

import com.cdasanpedro.application.dto.notificacion.NotificacionResponseDto;
import com.cdasanpedro.application.usecase.notificacion.NotificacionService;
import com.cdasanpedro.web.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificacionResponseDto>>> listarNotificaciones() {
        List<NotificacionResponseDto> list = notificacionService.listarNotificaciones();
        return ResponseEntity.ok(ApiResponse.ok(list, "Cola de notificaciones recuperada exitosamente"));
    }

    @PostMapping("/barrido")
    public ResponseEntity<ApiResponse<String>> ejecutarBarrido() {
        notificacionService.procesarBarridoDiario();
        return ResponseEntity.ok(ApiResponse.ok("Barrido de notificaciones ejecutado", "Proceso completado"));
    }

    @PostMapping("/{id}/reintentar")
    public ResponseEntity<ApiResponse<NotificacionResponseDto>> reintentar(@PathVariable UUID id) {
        NotificacionResponseDto dto = notificacionService.reintentar(id);
        return ResponseEntity.ok(ApiResponse.ok(dto, "Reintento de notificación procesado"));
    }

    @PostMapping("/test-email")
    public ResponseEntity<ApiResponse<String>> enviarCorreoPrueba(@jakarta.validation.Valid @RequestBody com.cdasanpedro.application.dto.notificacion.TestEmailRequestDto request) {
        notificacionService.enviarCorreoPrueba(request.getEmail(), request.getMensaje());
        return ResponseEntity.ok(ApiResponse.ok("Despachando correo de prueba a " + request.getEmail(), "Correo en proceso de entrega"));
    }

    @PostMapping("/enviar-plantilla-real")
    public ResponseEntity<ApiResponse<String>> enviarPlantillaReal(@jakarta.validation.Valid @RequestBody com.cdasanpedro.application.dto.notificacion.SendRealEmailRequestDto request) {
        String msg = notificacionService.enviarPlantillaReal(request);
        return ResponseEntity.ok(ApiResponse.ok(msg, "Plantilla transaccional enviada exitosamente"));
    }
}
