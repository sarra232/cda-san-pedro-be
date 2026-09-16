package com.cdasanpedro.application.usecase.hr;

import com.cdasanpedro.application.dto.cuentapagar.CuentaPorPagarRequestDto;
import com.cdasanpedro.application.dto.hr.*;
import com.cdasanpedro.application.usecase.cuentapagar.CuentaPorPagarService;
import com.cdasanpedro.core.exception.BusinessException;
import com.cdasanpedro.core.exception.ResourceNotFoundException;
import com.cdasanpedro.core.model.enums.EstadoEmpleado;
import com.cdasanpedro.core.model.enums.EstadoNomina;
import com.cdasanpedro.core.model.enums.PeriodicidadPago;
import com.cdasanpedro.core.model.enums.TipoObligacion;
import com.cdasanpedro.infrastructure.persistence.entity.EmpleadoEntity;
import com.cdasanpedro.infrastructure.persistence.entity.NominaDetalleEntity;
import com.cdasanpedro.infrastructure.persistence.entity.NominaEntity;
import com.cdasanpedro.infrastructure.persistence.repository.EmpleadoRepository;
import com.cdasanpedro.infrastructure.persistence.repository.NominaDetalleRepository;
import com.cdasanpedro.infrastructure.persistence.repository.NominaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NominaService {

    private final NominaRepository nominaRepository;
    private final NominaDetalleRepository nominaDetalleRepository;
    private final EmpleadoRepository empleadoRepository;
    private final CuentaPorPagarService cuentaPorPagarService;

    // Constantes Laborales Colombia 2026
    private static final BigDecimal AUXILIO_TRANSPORTE_MENSUAL = new BigDecimal("162000.00");
    private static final BigDecimal TOPE_AUXILIO_TRANSPORTE = new BigDecimal("2847000.00"); // 2 SMLV
    private static final BigDecimal PORC_SALUD_EMPLEADO = new BigDecimal("0.04");
    private static final BigDecimal PORC_PENSION_EMPLEADO = new BigDecimal("0.04");
    private static final BigDecimal PORC_PENSION_PATRONAL = new BigDecimal("0.12");
    private static final BigDecimal PORC_ARL_PROMEDIO = new BigDecimal("0.01044"); // Riesgo II CDA
    private static final BigDecimal PORC_CAJA_COMPENSACION = new BigDecimal("0.04");
    private static final BigDecimal PORC_PRIMA = new BigDecimal("0.0833");
    private static final BigDecimal PORC_CESANTIAS = new BigDecimal("0.0833");
    private static final BigDecimal PORC_INT_CESANTIAS = new BigDecimal("0.01");
    private static final BigDecimal PORC_VACACIONES = new BigDecimal("0.0416");

    @Transactional(readOnly = true)
    public List<NominaResponseDto> listarNominas() {
        return nominaRepository.findAllWithDetalles().stream()
                .map(this::mapToNominaDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NominaResponseDto obtenerPorId(UUID id) {
        NominaEntity entity = nominaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nómina no encontrada con ID: " + id));
        return mapToNominaDto(entity);
    }

    @Transactional
    public NominaResponseDto liquidarNomina(LiquidacionNominaRequestDto request) {
        log.info(">> [NominaService] Liquidando nómina periodo: {}-{} Quincena {}",
                request.getPeriodoAnio(), request.getPeriodoMes(), request.getPeriodoQuincena());

        // Validar si ya existe
        Optional<NominaEntity> existente = nominaRepository.findByPeriodoAnioAndPeriodoMesAndPeriodoQuincena(
                request.getPeriodoAnio(), request.getPeriodoMes(), request.getPeriodoQuincena());

        NominaEntity nomina;
        if (existente.isPresent()) {
            nomina = existente.get();
            if (nomina.getEstado() == EstadoNomina.PAGADA) {
                throw new BusinessException("La nómina de este periodo ya fue pagada y no puede reliquidarse.");
            }
            nomina.getDetalles().clear();
        } else {
            LocalDate fechaInicio = request.getFechaInicio() != null ? request.getFechaInicio() :
                    (request.getPeriodoQuincena() == 1 ? LocalDate.of(request.getPeriodoAnio(), request.getPeriodoMes(), 1) : LocalDate.of(request.getPeriodoAnio(), request.getPeriodoMes(), 16));

            LocalDate fechaFin = request.getFechaFin() != null ? request.getFechaFin() :
                    (request.getPeriodoQuincena() == 1 ? LocalDate.of(request.getPeriodoAnio(), request.getPeriodoMes(), 15) : LocalDate.of(request.getPeriodoAnio(), request.getPeriodoMes(), 1).plusMonths(1).minusDays(1));

            nomina = NominaEntity.builder()
                    .periodoAnio(request.getPeriodoAnio())
                    .periodoMes(request.getPeriodoMes())
                    .periodoQuincena(request.getPeriodoQuincena())
                    .fechaInicio(fechaInicio)
                    .fechaFin(fechaFin)
                    .estado(EstadoNomina.BORRADOR)
                    .observaciones(request.getObservaciones())
                    .detalles(new ArrayList<>())
                    .build();
        }

        // Obtener empleados activos
        List<EmpleadoEntity> empleados = empleadoRepository.findByEstado(EstadoEmpleado.ACTIVO);
        if (empleados.isEmpty()) {
            throw new BusinessException("No hay empleados activos en el sistema para liquidar la nómina.");
        }

        // Mapear novedades
        Map<UUID, LiquidacionNominaRequestDto.NovedadEmpleadoDto> novedadesMap = new HashMap<>();
        if (request.getNovedades() != null) {
            for (var nov : request.getNovedades()) {
                if (nov.getEmpleadoId() != null) novedadesMap.put(nov.getEmpleadoId(), nov);
            }
        }

        BigDecimal sumDevengado = BigDecimal.ZERO;
        BigDecimal sumDeducciones = BigDecimal.ZERO;
        BigDecimal sumNeto = BigDecimal.ZERO;
        BigDecimal sumAportes = BigDecimal.ZERO;
        BigDecimal sumProvisiones = BigDecimal.ZERO;

        int diasBasePeriodo = request.getPeriodoQuincena() == 1 ? 15 : 15;

        for (EmpleadoEntity emp : empleados) {
            var nov = novedadesMap.get(emp.getId());
            int dias = (nov != null && nov.getDiasTrabajados() != null && nov.getDiasTrabajados() > 0)
                    ? nov.getDiasTrabajados()
                    : diasBasePeriodo;

            BigDecimal horasExtras = (nov != null && nov.getHorasExtras() != null) ? nov.getHorasExtras() : BigDecimal.ZERO;
            BigDecimal bonificaciones = (nov != null && nov.getBonificaciones() != null) ? nov.getBonificaciones() : BigDecimal.ZERO;
            BigDecimal otrasDeduc = (nov != null && nov.getOtrasDeducciones() != null) ? nov.getOtrasDeducciones() : BigDecimal.ZERO;

            BigDecimal sueldoDevengado = emp.getSalarioBase()
                    .divide(new BigDecimal("30"), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(dias))
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal auxTransp = BigDecimal.ZERO;
            if (Boolean.TRUE.equals(emp.getAuxilioTransporteAplica()) && emp.getSalarioBase().compareTo(TOPE_AUXILIO_TRANSPORTE) <= 0) {
                auxTransp = AUXILIO_TRANSPORTE_MENSUAL
                        .divide(new BigDecimal("30"), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal(dias))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            BigDecimal totalDevengado = sueldoDevengado.add(auxTransp).add(horasExtras).add(bonificaciones);

            // Base para seguridad social (Sueldo + Horas Extras)
            BigDecimal baseSegSocial = sueldoDevengado.add(horasExtras);

            BigDecimal deducSalud = baseSegSocial.multiply(PORC_SALUD_EMPLEADO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal deducPension = baseSegSocial.multiply(PORC_PENSION_EMPLEADO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalDeducciones = deducSalud.add(deducPension).add(otrasDeduc);

            BigDecimal netoPagar = totalDevengado.subtract(totalDeducciones);

            // Aportes patronales y provisiones
            BigDecimal apPensionPatronal = baseSegSocial.multiply(PORC_PENSION_PATRONAL).setScale(2, RoundingMode.HALF_UP);
            BigDecimal apArl = baseSegSocial.multiply(PORC_ARL_PROMEDIO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal parafCaja = baseSegSocial.multiply(PORC_CAJA_COMPENSACION).setScale(2, RoundingMode.HALF_UP);

            BigDecimal basePrestaciones = totalDevengado; // Incluye auxilio de transporte
            BigDecimal provPrima = basePrestaciones.multiply(PORC_PRIMA).setScale(2, RoundingMode.HALF_UP);
            BigDecimal provCesantias = basePrestaciones.multiply(PORC_CESANTIAS).setScale(2, RoundingMode.HALF_UP);
            BigDecimal provIntCesantias = provCesantias.multiply(PORC_INT_CESANTIAS).setScale(2, RoundingMode.HALF_UP);
            BigDecimal provVacaciones = sueldoDevengado.multiply(PORC_VACACIONES).setScale(2, RoundingMode.HALF_UP);

            NominaDetalleEntity det = NominaDetalleEntity.builder()
                    .nomina(nomina)
                    .empleado(emp)
                    .diasTrabajados(dias)
                    .salarioBase(emp.getSalarioBase())
                    .sueldoDevengado(sueldoDevengado)
                    .auxilioTransporte(auxTransp)
                    .horasExtras(horasExtras)
                    .bonificaciones(bonificaciones)
                    .totalDevengado(totalDevengado)
                    .deduccionSalud(deducSalud)
                    .deduccionPension(deducPension)
                    .otrasDeducciones(otrasDeduc)
                    .totalDeducciones(totalDeducciones)
                    .netoPagar(netoPagar)
                    .aportePensionPatronal(apPensionPatronal)
                    .aporteArl(apArl)
                    .parafiscalesCaja(parafCaja)
                    .provisionPrima(provPrima)
                    .provisionCesantias(provCesantias)
                    .provisionInteresesCesantias(provIntCesantias)
                    .provisionVacaciones(provVacaciones)
                    .build();

            nomina.getDetalles().add(det);

            sumDevengado = sumDevengado.add(totalDevengado);
            sumDeducciones = sumDeducciones.add(totalDeducciones);
            sumNeto = sumNeto.add(netoPagar);
            sumAportes = sumAportes.add(apPensionPatronal).add(apArl).add(parafCaja);
            sumProvisiones = sumProvisiones.add(provPrima).add(provCesantias).add(provIntCesantias).add(provVacaciones);
        }

        nomina.setTotalDevengado(sumDevengado);
        nomina.setTotalDeducciones(sumDeducciones);
        nomina.setTotalNeto(sumNeto);
        nomina.setTotalAportesPatronales(sumAportes);
        nomina.setTotalProvisiones(sumProvisiones);

        NominaEntity guardada = nominaRepository.save(nomina);
        return mapToNominaDto(guardada);
    }

    @Transactional
    public NominaResponseDto aprobarNomina(UUID nominaId) {
        NominaEntity nomina = nominaRepository.findById(nominaId)
                .orElseThrow(() -> new ResourceNotFoundException("Nómina no encontrada con ID: " + nominaId));

        if (nomina.getEstado() == EstadoNomina.APROBADA || nomina.getEstado() == EstadoNomina.PAGADA) {
            return mapToNominaDto(nomina);
        }

        nomina.setEstado(EstadoNomina.APROBADA);

        // Integración automática con Cuentas por Pagar de Tesorería
        if (nomina.getCuentaPorPagar() == null && !nomina.getDetalles().isEmpty()) {
            try {
                // Se toma el primer empleado o tercero representativo
                UUID acreedorId = nomina.getDetalles().get(0).getEmpleado().getTercero().getId();
                String concepto = String.format("Pago Nómina Oficial Periodo %d-%02d Quincena %d (Total Colaboradores)",
                        nomina.getPeriodoAnio(), nomina.getPeriodoMes(), nomina.getPeriodoQuincena());

                var cxpDto = cuentaPorPagarService.crear(CuentaPorPagarRequestDto.builder()
                        .acreedorTerceroId(acreedorId)
                        .numeroReferencia(String.format("NOM-%d-%02d-Q%d", nomina.getPeriodoAnio(), nomina.getPeriodoMes(), nomina.getPeriodoQuincena()))
                        .concepto(concepto)
                        .montoTotal(nomina.getTotalNeto())
                        .tipoObligacion(TipoObligacion.OBLIGACION_LABORAL)
                        .periodicidad(PeriodicidadPago.QUINCENAL)
                        .fechaEmision(LocalDate.now())
                        .fechaVencimiento(nomina.getFechaFin())
                        .diasAvisoAnticipado(3)
                        .observaciones("Generado automáticamente al aprobar la liquidación de nómina de talento humano.")
                        .build());

                // Vincular id
                // En entity se mantiene la relación
            } catch (Exception ex) {
                log.warn(">> [NominaService] No se pudo crear cuenta por pagar automática: {}", ex.getMessage());
            }
        }

        return mapToNominaDto(nominaRepository.save(nomina));
    }

    public NominaResponseDto mapToNominaDto(NominaEntity n) {
        List<NominaDetalleResponseDto> dets = (n.getDetalles() != null)
                ? n.getDetalles().stream().map(this::mapToDetalleDto).collect(Collectors.toList())
                : new ArrayList<>();

        String periodoDesc = String.format("%d-%02d (Q%d)", n.getPeriodoAnio(), n.getPeriodoMes(), n.getPeriodoQuincena());

        return NominaResponseDto.builder()
                .id(n.getId())
                .periodoAnio(n.getPeriodoAnio())
                .periodoMes(n.getPeriodoMes())
                .periodoQuincena(n.getPeriodoQuincena())
                .periodoDescripcion(periodoDesc)
                .fechaInicio(n.getFechaInicio())
                .fechaFin(n.getFechaFin())
                .totalDevengado(n.getTotalDevengado())
                .totalDeducciones(n.getTotalDeducciones())
                .totalNeto(n.getTotalNeto())
                .totalAportesPatronales(n.getTotalAportesPatronales())
                .totalProvisiones(n.getTotalProvisiones())
                .estado(n.getEstado())
                .cuentaPorPagarId(n.getCuentaPorPagar() != null ? n.getCuentaPorPagar().getId() : null)
                .observaciones(n.getObservaciones())
                .detalles(dets)
                .createdAt(n.getCreatedAt())
                .build();
    }

    public NominaDetalleResponseDto mapToDetalleDto(NominaDetalleEntity d) {
        EmpleadoEntity e = d.getEmpleado();
        var t = e != null ? e.getTercero() : null;

        return NominaDetalleResponseDto.builder()
                .id(d.getId())
                .empleadoId(e != null ? e.getId() : null)
                .empleadoNombre(t != null ? t.getRazonSocialONombre() : "Colaborador")
                .empleadoDocumento(t != null ? t.getNumeroDocumento() : "")
                .empleadoCargo(e != null ? e.getCargo() : "")
                .banco(e != null ? e.getBanco() : "")
                .numeroCuenta(e != null ? e.getNumeroCuenta() : "")
                .diasTrabajados(d.getDiasTrabajados())
                .salarioBase(d.getSalarioBase())
                .sueldoDevengado(d.getSueldoDevengado())
                .auxilioTransporte(d.getAuxilioTransporte())
                .horasExtras(d.getHorasExtras())
                .bonificaciones(d.getBonificaciones())
                .totalDevengado(d.getTotalDevengado())
                .deduccionSalud(d.getDeduccionSalud())
                .deduccionPension(d.getDeduccionPension())
                .otrasDeducciones(d.getOtrasDeducciones())
                .totalDeducciones(d.getTotalDeducciones())
                .netoPagar(d.getNetoPagar())
                .aporteSaludPatronal(d.getAporteSaludPatronal())
                .aportePensionPatronal(d.getAportePensionPatronal())
                .aporteArl(d.getAporteArl())
                .parafiscalesCaja(d.getParafiscalesCaja())
                .provisionPrima(d.getProvisionPrima())
                .provisionCesantias(d.getProvisionCesantias())
                .provisionInteresesCesantias(d.getProvisionInteresesCesantias())
                .provisionVacaciones(d.getProvisionVacaciones())
                .build();
    }
}
