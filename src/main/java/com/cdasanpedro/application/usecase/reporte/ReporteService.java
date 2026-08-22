package com.cdasanpedro.application.usecase.reporte;

import com.cdasanpedro.application.dto.factura.FacturaResponseDto;
import com.cdasanpedro.application.dto.reporte.ActividadHorariaDto;
import com.cdasanpedro.application.dto.reporte.DashboardStatsDto;
import com.cdasanpedro.application.dto.reporte.ReporteVentasDto;
import com.cdasanpedro.application.usecase.factura.FacturaService;
import com.cdasanpedro.application.usecase.vehiculo.VehiculoService;
import com.cdasanpedro.core.model.enums.CategoriaVehiculo;
import com.cdasanpedro.infrastructure.persistence.entity.FacturaEntity;
import com.cdasanpedro.infrastructure.persistence.entity.OrdenIngresoEntity;
import com.cdasanpedro.infrastructure.persistence.entity.VehiculoEntity;
import com.cdasanpedro.infrastructure.persistence.repository.FacturaRepository;
import com.cdasanpedro.infrastructure.persistence.repository.OrdenIngresoRepository;
import com.cdasanpedro.infrastructure.persistence.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final FacturaRepository facturaRepository;
    private final OrdenIngresoRepository ordenIngresoRepository;
    private final VehiculoRepository vehiculoRepository;
    private final FacturaService facturaService;
    private final VehiculoService vehiculoService;

    @Transactional(readOnly = true)
    public DashboardStatsDto obtenerDashboardStats() {
        LocalDate hoy = LocalDate.now();
        OffsetDateTime start = hoy.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime end = hoy.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        List<OrdenIngresoEntity> ordenesHoy = ordenIngresoRepository.findByFechaIngresoBetween(start, end);
        List<FacturaEntity> facturasHoy = facturaRepository.findByFechaEmisionBetween(start, end);

        BigDecimal recaudoHoy = facturasHoy.stream()
                .map(FacturaEntity::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Contadores por categoría de vehículo hoy
        long totalMotos = ordenesHoy.stream()
                .filter(o -> o.getVehiculo() != null && CategoriaVehiculo.MOTO.equals(o.getVehiculo().getCategoria()))
                .count();

        long totalLivianos = ordenesHoy.stream()
                .filter(o -> o.getVehiculo() != null && CategoriaVehiculo.LIVIANO.equals(o.getVehiculo().getCategoria()))
                .count();

        long totalPesados = ordenesHoy.stream()
                .filter(o -> o.getVehiculo() != null && CategoriaVehiculo.PESADO.equals(o.getVehiculo().getCategoria()))
                .count();

        long totalPublicos = ordenesHoy.stream()
                .filter(o -> o.getVehiculo() != null && CategoriaVehiculo.PUBLICO.equals(o.getVehiculo().getCategoria()))
                .count();

        // Alertas de vencimiento de SOAT/RTM activas
        List<VehiculoEntity> todosVehiculos = vehiculoRepository.findAll();
        long alertasVencimiento = todosVehiculos.stream()
                .filter(v -> {
                    var dto = vehiculoService.toDto(v);
                    return dto != null && (dto.isSoatVencido() || dto.isRtmVencido() || dto.isSoatProximoVencer() || dto.isRtmProximoVencer());
                })
                .count();

        // Actividad por bloques de horas hoy
        List<ActividadHorariaDto> actividadPorHoras = construirActividadHoraria(ordenesHoy, facturasHoy);

        return DashboardStatsDto.builder()
                .recaudoHoy(recaudoHoy)
                .vehiculosAtendidosHoy((long) ordenesHoy.size())
                .facturasEmitidasHoy((long) facturasHoy.size())
                .alertasVencimiento(alertasVencimiento)
                .totalMotos(totalMotos)
                .totalLivianos(totalLivianos)
                .totalPesados(totalPesados)
                .totalPublicos(totalPublicos)
                .actividadPorHoras(actividadPorHoras)
                .build();
    }

    @Transactional(readOnly = true)
    public ReporteVentasDto generarReporteVentas(LocalDate fechaInicio, LocalDate fechaFin) {
        LocalDate startDay = fechaInicio != null ? fechaInicio : LocalDate.now().minusDays(30);
        LocalDate endDay = fechaFin != null ? fechaFin : LocalDate.now();

        OffsetDateTime start = startDay.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime end = endDay.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        List<FacturaEntity> facturas = facturaRepository.findByFechaEmisionBetween(start, end);

        BigDecimal totalRecaudado = facturas.stream()
                .map(FacturaEntity::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> vehiculosPorCategoria = new HashMap<>();
        Map<String, BigDecimal> ingresosPorMetodo = new HashMap<>();

        for (FacturaEntity f : facturas) {
            // Categoría
            if (f.getOrdenIngreso() != null && f.getOrdenIngreso().getVehiculo() != null) {
                String cat = f.getOrdenIngreso().getVehiculo().getCategoria().name();
                vehiculosPorCategoria.put(cat, vehiculosPorCategoria.getOrDefault(cat, 0L) + 1);
            }

            // Método de Pago
            String met = f.getMetodoPago().name();
            ingresosPorMetodo.put(met, ingresosPorMetodo.getOrDefault(met, BigDecimal.ZERO).add(f.getTotal()));
        }

        List<FacturaResponseDto> facturasDto = facturas.stream()
                .map(facturaService::toDto)
                .collect(Collectors.toList());

        return ReporteVentasDto.builder()
                .fechaInicio(startDay)
                .fechaFin(endDay)
                .totalRecaudado(totalRecaudado)
                .totalVehiculos((long) facturas.size())
                .totalFacturas((long) facturas.size())
                .vehiculosPorCategoria(vehiculosPorCategoria)
                .ingresosPorMetodoPago(ingresosPorMetodo)
                .facturas(facturasDto)
                .build();
    }

    public String generarCsvVentas(LocalDate fechaInicio, LocalDate fechaFin) {
        ReporteVentasDto reporte = generarReporteVentas(fechaInicio, fechaFin);
        StringBuilder sb = new StringBuilder();

        // Encabezados CSV con formato compatible Excel
        sb.append("Numero Factura;Fecha Emision;Placa;Categoria;Cliente;Documento;Metodo Pago;Subtotal;IVA;Total\n");

        for (FacturaResponseDto f : reporte.getFacturas()) {
            sb.append(f.getNumeroFactura()).append(";")
                    .append(f.getFechaEmision() != null ? f.getFechaEmision().toLocalDate() : "").append(";")
                    .append(f.getOrdenIngreso() != null && f.getOrdenIngreso().getVehiculo() != null ? f.getOrdenIngreso().getVehiculo().getPlaca() : "").append(";")
                    .append(f.getOrdenIngreso() != null && f.getOrdenIngreso().getVehiculo() != null ? f.getOrdenIngreso().getVehiculo().getCategoria() : "").append(";")
                    .append(f.getClienteFactura() != null ? f.getClienteFactura().getNombresRazonSocial().replace(";", " ") : "").append(";")
                    .append(f.getClienteFactura() != null ? f.getClienteFactura().getNumeroDocumento() : "").append(";")
                    .append(f.getMetodoPago()).append(";")
                    .append(f.getSubtotal()).append(";")
                    .append(f.getIva()).append(";")
                    .append(f.getTotal()).append("\n");
        }

        return sb.toString();
    }

    private List<ActividadHorariaDto> construirActividadHoraria(List<OrdenIngresoEntity> ordenes, List<FacturaEntity> facturas) {
        String[] bloques = {"08:00", "10:00", "12:00", "14:00", "16:00", "18:00"};
        List<ActividadHorariaDto> lista = new ArrayList<>();

        for (String b : bloques) {
            int horaBloque = Integer.parseInt(b.split(":")[0]);

            long countVeh = ordenes.stream()
                    .filter(o -> o.getFechaIngreso() != null && o.getFechaIngreso().getHour() >= horaBloque && o.getFechaIngreso().getHour() < horaBloque + 2)
                    .count();

            BigDecimal sumIngresos = facturas.stream()
                    .filter(f -> f.getFechaEmision() != null && f.getFechaEmision().getHour() >= horaBloque && f.getFechaEmision().getHour() < horaBloque + 2)
                    .map(FacturaEntity::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            lista.add(ActividadHorariaDto.builder()
                    .name(b)
                    .vehiculos(countVeh)
                    .ingresos(sumIngresos)
                    .build());
        }

        return lista;
    }
}
