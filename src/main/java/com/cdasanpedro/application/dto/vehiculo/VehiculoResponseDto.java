package com.cdasanpedro.application.dto.vehiculo;

import com.cdasanpedro.application.dto.cliente.ClienteResponseDto;
import com.cdasanpedro.core.model.enums.CategoriaVehiculo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehiculoResponseDto {

    private UUID id;
    private String placa;
    private CategoriaVehiculo categoria;
    private String marca;
    private String linea;
    private Integer modelo;
    private String chasisVin;
    private LocalDate fechaVencimientoSoat;
    private LocalDate fechaVencimientoRtm;
    private boolean soatVencido;
    private boolean rtmVencido;
    private boolean soatProximoVencer;
    private boolean rtmProximoVencer;
    private ClienteResponseDto propietario;
    private OffsetDateTime createdAt;

    public static VehiculoResponseDto fromEntity(
            com.cdasanpedro.infrastructure.persistence.entity.VehiculoEntity entity,
            ClienteResponseDto propietarioDto
    ) {
        LocalDate hoy = LocalDate.now();
        LocalDate limiteAlerta = hoy.plusDays(15);

        boolean soatVencido = entity.getFechaVencimientoSoat() != null && entity.getFechaVencimientoSoat().isBefore(hoy);
        boolean soatProximo = entity.getFechaVencimientoSoat() != null && !soatVencido && !entity.getFechaVencimientoSoat().isAfter(limiteAlerta);

        boolean rtmVencido = entity.getFechaVencimientoRtm() != null && entity.getFechaVencimientoRtm().isBefore(hoy);
        boolean rtmProximo = entity.getFechaVencimientoRtm() != null && !rtmVencido && !entity.getFechaVencimientoRtm().isAfter(limiteAlerta);

        return VehiculoResponseDto.builder()
                .id(entity.getId())
                .placa(entity.getPlaca())
                .categoria(entity.getCategoria())
                .marca(entity.getMarca())
                .linea(entity.getLinea())
                .modelo(entity.getModelo())
                .chasisVin(entity.getChasisVin())
                .fechaVencimientoSoat(entity.getFechaVencimientoSoat())
                .fechaVencimientoRtm(entity.getFechaVencimientoRtm())
                .soatVencido(soatVencido)
                .soatProximoVencer(soatProximo)
                .rtmVencido(rtmVencido)
                .rtmProximoVencer(rtmProximo)
                .propietario(propietarioDto)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
