package com.cdasanpedro.web.controller;

import com.cdasanpedro.application.dto.cliente.ClienteRequestDto;
import com.cdasanpedro.application.dto.cliente.ClienteResponseDto;
import com.cdasanpedro.application.usecase.cliente.ClienteService;
import com.cdasanpedro.core.exception.ResourceNotFoundException;
import com.cdasanpedro.web.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @PostMapping
    public ResponseEntity<ApiResponse<ClienteResponseDto>> registrarOActualizar(@Valid @RequestBody ClienteRequestDto request) {
        ClienteResponseDto response = clienteService.registrarOActualizar(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Cliente procesado exitosamente"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClienteResponseDto>>> buscarClientes(
            @RequestParam(name = "query", required = false) String query
    ) {
        List<ClienteResponseDto> clientes = clienteService.buscarClientes(query);
        return ResponseEntity.ok(ApiResponse.ok(clientes, "Clientes recuperados"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClienteResponseDto>> obtenerPorId(@PathVariable UUID id) {
        ClienteResponseDto cliente = clienteService.obtenerPorId(id);
        return ResponseEntity.ok(ApiResponse.ok(cliente, "Cliente encontrado"));
    }

    @GetMapping("/documento/{numeroDocumento}")
    public ResponseEntity<ApiResponse<ClienteResponseDto>> buscarPorDocumento(@PathVariable String numeroDocumento) {
        ClienteResponseDto cliente = clienteService.buscarPorDocumento(numeroDocumento)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró cliente con documento: " + numeroDocumento));
        return ResponseEntity.ok(ApiResponse.ok(cliente, "Cliente encontrado"));
    }
}
