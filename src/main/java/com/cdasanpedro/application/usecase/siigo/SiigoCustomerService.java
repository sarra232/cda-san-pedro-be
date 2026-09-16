package com.cdasanpedro.application.usecase.siigo;

import com.cdasanpedro.application.dto.siigo.SiigoCustomerRequestDto;
import com.cdasanpedro.application.dto.siigo.SiigoCustomerResponseDto;
import com.cdasanpedro.application.dto.siigo.SiigoCustomerListResponseDto;
import com.cdasanpedro.application.dto.siigo.SiigoSyncResultDto;
import com.cdasanpedro.core.model.enums.TipoDocumento;
import com.cdasanpedro.infrastructure.persistence.entity.ClienteEntity;
import com.cdasanpedro.infrastructure.persistence.entity.TerceroEntity;
import com.cdasanpedro.infrastructure.siigo.client.SiigoApiClient;
import com.cdasanpedro.infrastructure.siigo.config.SiigoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiigoCustomerService {

    private final SiigoApiClient apiClient;
    private final SiigoAuthService authService;
    private final SiigoProperties properties;

    public SiigoCustomerResponseDto sincronizarCliente(ClienteEntity cliente) {
        if (cliente == null || cliente.getNumeroDocumento() == null) {
            throw new IllegalArgumentException("Cliente pagador no válido para sincronización fiscal con SIIGO.");
        }

        String idType = mapTipoDocumento(cliente.getTipoDocumento());
        boolean esEmpresa = cliente.getTipoDocumento() == TipoDocumento.NIT;
        String personType = esEmpresa ? "Company" : "Person";
        String rawDoc = cliente.getNumeroDocumento().trim();
        String cleanDoc = rawDoc.replace(".", "").replace(" ", "").replace("-", "").trim();
        String checkDigit = null;

        if (esEmpresa && rawDoc.contains("-")) {
            String[] parts = rawDoc.split("-");
            cleanDoc = parts[0].replaceAll("[^0-9]", "").trim();
            if (parts.length > 1 && !parts[1].trim().isBlank()) {
                checkDigit = parts[1].replaceAll("[^0-9]", "").trim();
            }
        }

        String nombreCompleto = cliente.getNombresRazonSocial() != null && !cliente.getNombresRazonSocial().trim().isBlank()
                ? cliente.getNombresRazonSocial().trim() 
                : "CLIENTE GENERAL";
        List<String> names;
        if (esEmpresa) {
            names = List.of(nombreCompleto);
        } else {
            String[] parts = nombreCompleto.split("\\s+", 2);
            if (parts.length > 1 && !parts[1].trim().isBlank()) {
                names = List.of(parts[0].trim(), parts[1].trim());
            } else {
                names = List.of(nombreCompleto.trim(), "Cliente");
            }
        }

        List<SiigoCustomerRequestDto.FiscalResponsibilityDto> fiscalList = esEmpresa
                ? List.of(SiigoCustomerRequestDto.FiscalResponsibilityDto.builder().code("O-48").build())
                : List.of(SiigoCustomerRequestDto.FiscalResponsibilityDto.builder().code("R-99-PN").build());

        // Sanitización estricta de teléfono: solo dígitos numéricos (máximo 10 caracteres)
        List<SiigoCustomerRequestDto.CustomerPhoneDto> phonesList;
        String rawPhone = (cliente.getCelular() != null && !cliente.getCelular().isBlank())
                ? cliente.getCelular()
                : "3000000000";
        String cleanPhone = rawPhone.replaceAll("[^0-9]", "").trim();
        if (cleanPhone.startsWith("57") && cleanPhone.length() > 10) {
            cleanPhone = cleanPhone.substring(2);
        }
        if (cleanPhone.length() > 10) {
            cleanPhone = cleanPhone.substring(cleanPhone.length() - 10);
        }
        if (cleanPhone.length() < 7) {
            cleanPhone = "3000000000";
        }
        phonesList = List.of(SiigoCustomerRequestDto.CustomerPhoneDto.builder()
                .indicative("57")
                .number(cleanPhone)
                .build());

        // Sanitización de contactos y correo electrónico DIAN
        String emailFinal = (cliente.getEmail() != null && !cliente.getEmail().isBlank() && cliente.getEmail().contains("@"))
                ? cliente.getEmail().trim().toLowerCase()
                : "facturacion@cdasanpedro.com";

        List<SiigoCustomerRequestDto.CustomerContactDto> contactsList = List.of(
                SiigoCustomerRequestDto.CustomerContactDto.builder()
                        .firstName(names.get(0))
                        .lastName(names.size() > 1 ? names.get(1) : "Cliente")
                        .email(emailFinal)
                        .phone(phonesList.get(0))
                        .build()
        );

        String direccionFinal = (cliente.getDireccion() != null && !cliente.getDireccion().isBlank())
                ? cliente.getDireccion().trim()
                : "Cra 50 # 48-20";

        SiigoCustomerRequestDto.SiigoCustomerRequestDtoBuilder builder = SiigoCustomerRequestDto.builder()
                .type("Customer")
                .personType(personType)
                .idType(idType)
                .identification(cleanDoc)
                .checkDigit(checkDigit)
                .name(names)
                .commercialName(nombreCompleto)
                .active(true)
                .vatResponsible(esEmpresa)
                .fiscalResponsibilities(fiscalList)
                .phones(phonesList)
                .contacts(contactsList)
                .address(SiigoCustomerRequestDto.CustomerAddressDto.builder()
                        .address(direccionFinal)
                        .city(SiigoCustomerRequestDto.CityDto.builder()
                                .countryCode("Co")
                                .stateCode("05")
                                .cityCode("05664")
                                .build())
                        .postalCode("051050")
                        .build());

        SiigoCustomerRequestDto request = builder.build();

        if (!properties.isConfigured() && properties.isSandbox()) {
            log.info(">> [SIIGO SANDBOX] Sincronización simulada exitosa para documento: {}", cleanDoc);
            return SiigoCustomerResponseDto.builder()
                    .id("sandbox_cust_" + cleanDoc)
                    .identification(cleanDoc)
                    .idType(idType)
                    .commercialName(nombreCompleto)
                    .active(true)
                    .build();
        }

        try {
            String token = authService.getValidToken();
            log.info(">> [SIIGO] Registrando cliente fiscal en SIIGO: Doc={}, Nombres={}, Phones={}", cleanDoc, names, phonesList.stream().map(p -> p.getIndicative() + "-" + p.getNumber()).toList());
            return apiClient.createCustomer(request, token);
        } catch (Exception e) {
            log.warn(">> [SIIGO] Error en creación de cliente {} ({}). Verificando si ya existe previamente en SIIGO...", cleanDoc, e.getMessage());
            try {
                String token = authService.getValidToken();
                SiigoCustomerListResponseDto busqueda = apiClient.getCustomerByIdentification(cleanDoc, token);
                if (busqueda != null && busqueda.getResults() != null && !busqueda.getResults().isEmpty()) {
                    SiigoCustomerListResponseDto.CustomerItemDto item = busqueda.getResults().get(0);
                    log.info(">> [SIIGO] Cliente {} localizado exitosamente en SIIGO con ID: {}", cleanDoc, item.getId());
                    return SiigoCustomerResponseDto.builder()
                            .id(item.getId())
                            .identification(item.getIdentification())
                            .idType(idType)
                            .commercialName(nombreCompleto)
                            .active(true)
                            .build();
                }
            } catch (Exception ex) {
                log.error(">> [SIIGO] Error al consultar existencia previa del cliente {}: {}", cleanDoc, ex.getMessage());
            }

            log.error(">> [SIIGO ERROR CRÍTICO] El cliente {} no pudo sincronizarse con SIIGO: {}", cleanDoc, e.getMessage());
            throw new IllegalStateException("Error al sincronizar cliente en SIIGO: " + e.getMessage(), e);
        }
    }

    private final com.cdasanpedro.infrastructure.persistence.repository.ClienteRepository clienteRepository;
    private final com.cdasanpedro.infrastructure.persistence.repository.TerceroRepository terceroRepository;

    public SiigoSyncResultDto importarClientesDesdeSiigo(int maxPaginas, int tamanoPagina) {
        log.info(">> [SIIGO] Iniciando importación masiva de clientes (Máx páginas: {}, Tamaño: {})", maxPaginas, tamanoPagina);
        
        int totalProcesados = 0;
        int nuevosCreados = 0;
        int actualizados = 0;
        int fallidos = 0;

        if (!properties.isConfigured() && properties.isSandbox()) {
            log.info(">> [SIIGO SANDBOX] Ejecutando importación simulada de clientes históricos.");
            return simularImportacionSandbox();
        }

        try {
            String token = authService.getValidToken();

            for (int page = 1; page <= maxPaginas; page++) {
                SiigoCustomerListResponseDto response = apiClient.getCustomers(page, tamanoPagina, token);

                if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                    log.info(">> [SIIGO] No hay más clientes por procesar en la página {}", page);
                    break;
                }

                for (SiigoCustomerListResponseDto.CustomerItemDto item : response.getResults()) {
                    try {
                        totalProcesados++;
                        boolean esNuevo = procesarYGuardarCliente(item);
                        if (esNuevo) nuevosCreados++;
                        else actualizados++;
                    } catch (Exception e) {
                        log.warn(">> [SIIGO] Error procesando cliente {}: {}", item.getIdentification(), e.getMessage());
                        fallidos++;
                    }
                }

                if (response.getPagination() != null && response.getPagination().getTotalResults() != null) {
                    if (page * tamanoPagina >= response.getPagination().getTotalResults()) {
                        break;
                    }
                }
            }

            return SiigoSyncResultDto.builder()
                    .totalProcesados(totalProcesados)
                    .nuevosCreados(nuevosCreados)
                    .actualizados(actualizados)
                    .fallidos(fallidos)
                    .mensaje(String.format("Sincronización completada: %d procesados (%d nuevos, %d actualizados, %d fallidos)",
                            totalProcesados, nuevosCreados, actualizados, fallidos))
                    .build();

        } catch (Exception e) {
            log.error(">> [SIIGO] Error en importación masiva: {}", e.getMessage());
            return SiigoSyncResultDto.builder()
                    .totalProcesados(totalProcesados)
                    .nuevosCreados(nuevosCreados)
                    .actualizados(actualizados)
                    .fallidos(fallidos + 1)
                    .mensaje("Error al sincronizar con SIIGO: " + e.getMessage())
                    .build();
        }
    }

    public java.util.Optional<ClienteEntity> buscarYAutoguardarDesdeSiigo(String numeroDocumento) {
        if (numeroDocumento == null || numeroDocumento.trim().isBlank()) {
            return java.util.Optional.empty();
        }

        String cleanDoc = numeroDocumento.trim();

        if (!properties.isConfigured() && properties.isSandbox()) {
            log.info(">> [SIIGO SANDBOX] Simulación de búsqueda JIT para doc: {}", cleanDoc);
            return java.util.Optional.empty();
        }

        try {
            String token = authService.getValidToken();
            SiigoCustomerListResponseDto response = apiClient.getCustomerByIdentification(cleanDoc, token);

            if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
                SiigoCustomerListResponseDto.CustomerItemDto item = response.getResults().get(0);
                procesarYGuardarCliente(item);
                return clienteRepository.findByNumeroDocumento(cleanDoc);
            }
        } catch (Exception e) {
            log.warn(">> [SIIGO] No fue posible consultar cliente en SIIGO por documento {}: {}", cleanDoc, e.getMessage());
        }

        return java.util.Optional.empty();
    }

    private boolean procesarYGuardarCliente(SiigoCustomerListResponseDto.CustomerItemDto item) {
        if (item.getIdentification() == null || item.getIdentification().isBlank()) {
            return false;
        }

        String doc = item.getIdentification().trim();
        java.util.Optional<ClienteEntity> existenteOpt = clienteRepository.findByNumeroDocumento(doc);
        boolean esNuevo = existenteOpt.isEmpty();

        ClienteEntity cliente = existenteOpt.orElseGet(() -> ClienteEntity.builder()
                .numeroDocumento(doc)
                .build());

        TipoDocumento tipoDoc = parseTipoDocumento(item.getIdType());
        cliente.setTipoDocumento(tipoDoc);

        String nombre = item.getCommercialName();
        if ((nombre == null || nombre.isBlank()) && item.getName() != null && !item.getName().isEmpty()) {
            nombre = String.join(" ", item.getName()).trim();
        }
        if (nombre == null || nombre.isBlank()) {
            nombre = "CLIENTE " + doc;
        }
        cliente.setNombresRazonSocial(nombre);

        if (item.getPhones() != null && !item.getPhones().isEmpty()) {
            String tel = item.getPhones().get(0).getNumber();
            if (tel != null && !tel.isBlank()) {
                cliente.setCelular(tel.trim());
            }
        }
        if (cliente.getCelular() == null || cliente.getCelular().isBlank()) {
            cliente.setCelular("3000000000");
        }

        if (item.getContacts() != null && !item.getContacts().isEmpty()) {
            String email = item.getContacts().get(0).getEmail();
            if (email != null && !email.isBlank()) {
                cliente.setEmail(email.trim().toLowerCase());
            }
        }

        if (item.getAddress() != null && item.getAddress().getAddress() != null) {
            cliente.setDireccion(item.getAddress().getAddress().trim());
        }

        clienteRepository.save(cliente);
        return esNuevo;
    }

    private TipoDocumento parseTipoDocumento(Object idTypeObj) {
        if (idTypeObj == null) return TipoDocumento.CC;
        String idTypeStr = idTypeObj.toString();
        if (idTypeStr.contains("31") || idTypeStr.toUpperCase().contains("NIT")) return TipoDocumento.NIT;
        if (idTypeStr.contains("22") || idTypeStr.toUpperCase().contains("CE") || idTypeStr.toUpperCase().contains("EXTRANJERIA")) return TipoDocumento.CE;
        if (idTypeStr.contains("41") || idTypeStr.toUpperCase().contains("PASAPORTE")) return TipoDocumento.PASAPORTE;
        if (idTypeStr.contains("12") || idTypeStr.toUpperCase().contains("TI")) return TipoDocumento.TI;
        return TipoDocumento.CC;
    }

    private SiigoSyncResultDto simularImportacionSandbox() {
        // Cargar 3 clientes de demostración en modo Sandbox si no existen
        String[][] demo = {
                {"1020304050", "CC", "Carlos Eduardo Ramírez Gómez", "3104567890", "carlos.ramirez@example.com", "Cra 15 # 45-20"},
                {"900123456-1", "NIT", "Transportes Rápidos San Pedro S.A.S.", "3158901234", "facturacion@transrapidossanpedro.com", "Calle 80 # 68-12"},
                {"1098765432", "CC", "María Fernanda Restrepo Vélez", "3187654321", "maria.restrepo@example.com", "Av. Santander # 12-40"}
        };

        int creados = 0;
        int actualizados = 0;

        for (String[] d : demo) {
            String doc = d[0].trim();
            java.util.Optional<ClienteEntity> opt = clienteRepository.findByNumeroDocumento(doc);
            boolean esNuevo = opt.isEmpty();

            ClienteEntity cliente = opt.orElseGet(() -> ClienteEntity.builder()
                    .numeroDocumento(doc)
                    .build());

            cliente.setTipoDocumento("NIT".equals(d[1]) ? TipoDocumento.NIT : TipoDocumento.CC);
            cliente.setNombresRazonSocial(d[2]);
            cliente.setCelular(d[3]);
            cliente.setEmail(d[4]);
            cliente.setDireccion(d[5]);

            clienteRepository.save(cliente);
            if (esNuevo) creados++;
            else actualizados++;
        }

        return SiigoSyncResultDto.builder()
                .totalProcesados(demo.length)
                .nuevosCreados(creados)
                .actualizados(actualizados)
                .fallidos(0)
                .mensaje(String.format("Sincronización Sandbox completada: %d procesados (%d nuevos creados, %d actualizados)",
                        demo.length, creados, actualizados))
                .build();
    }

    public String mapTipoDocumento(TipoDocumento tipo) {
        if (tipo == null) return "13";
        return switch (tipo) {
            case CC -> "13";
            case NIT -> "31";
            case CE -> "22";
            case PASAPORTE -> "41";
            case TI -> "12";
            default -> "13";
        };
    }
}
