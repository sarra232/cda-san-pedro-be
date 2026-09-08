package com.cdasanpedro.infrastructure.notification;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class EmailTemplateBuilder {

    private static final NumberFormat CURRENCY_COP = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", new Locale("es", "CO"));
    private static final String LOGO_SRC = "cid:logoCda";

    static {
        CURRENCY_COP.setMaximumFractionDigits(0);
    }

    private static String getHeaderHtml(String subtitle) {
        return """
            <div style="background-color: #0A0A0A; padding: 26px 20px 18px 20px; text-align: center; border-bottom: 3px solid #F59E0B;">
                <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%">
                    <tr>
                        <td align="center" style="padding-bottom: 8px;">
                            <div style="display: inline-block; background: linear-gradient(135deg, #1E293B 0%, #0F172A 100%); border: 2px solid #F59E0B; border-radius: 12px; padding: 10px 24px; box-shadow: 0 4px 15px rgba(245, 158, 11, 0.35);">
                                <table role="presentation" border="0" cellpadding="0" cellspacing="0">
                                    <tr>
                                        <td style="vertical-align: middle; padding-right: 12px;">
                                            <span style="font-size: 26px; line-height: 1;">🛡️</span>
                                        </td>
                                        <td style="vertical-align: middle; text-align: left;">
                                            <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; font-size: 22px; font-weight: 900; line-height: 1.1; letter-spacing: 2.5px;">
                                                <span style="color: #F59E0B;">CDA </span><span style="color: #FFFFFF;">SAN PEDRO</span>
                                            </div>
                                            <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; font-size: 9.5px; font-weight: 700; color: #94A3B8; letter-spacing: 1.5px; margin-top: 3px; text-transform: uppercase;">
                                                Centro de Diagnóstico Automotor
                                            </div>
                                        </td>
                                    </tr>
                                </table>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td align="center" style="padding-top: 6px;">
                            <p style="margin: 0; font-size: 11px; color: #F59E0B; font-weight: 700; letter-spacing: 1px; text-transform: uppercase;">{{SUBTITLE}}</p>
                        </td>
                    </tr>
                </table>
            </div>
            """.replace("{{SUBTITLE}}", subtitle);
    }

    private static String getFooterHtml() {
        return """
            <div style="background-color: #0A0A0A; padding: 22px 20px; text-align: center; font-size: 11px; color: #64748B; border-top: 1px solid #1E293B;">
                <p style="margin: 0 0 4px 0; color: #94A3B8; font-weight: 700;">CDA San Pedro S.A.S. • NIT 901.558.942-1</p>
                <p style="margin: 0 0 6px 0;">Cra. 50 # 48-20, San Pedro de los Milagros, Antioquia • Tel: (604) 868 6060 • Cel/WhatsApp: 311 345 6789</p>
                <p style="margin: 0; color: #475569;">Centro de Diagnóstico Automotor Habilitado por el Ministerio de Transporte y RUNT</p>
            </div>
            """;
    }

    /**
     * 1. Plantilla: Recordatorio Preventivo de Vencimiento de Tecnomecánica RTM
     */
    public static String buildRecordatorioRtm(String nombreCliente, String placa, String categoria, LocalDate fechaVencimiento, int diasRestantes) {
        String fechaStr = fechaVencimiento != null ? fechaVencimiento.format(DATE_FORMATTER) : "";
        String badgeDias = diasRestantes <= 0 ? "¡TU REVISIÓN VENCIÓ HOY!" : "VENCE EN " + diasRestantes + " DÍAS";
        String colorBadge = diasRestantes <= 5 ? "#EF4444" : "#F59E0B";
        String placaUpper = placa != null && !placa.isBlank() ? placa.toUpperCase() : "";
        String nombre = nombreCliente != null && !nombreCliente.isBlank() ? nombreCliente : "";
        String cat = categoria != null && !categoria.isBlank() ? categoria : "";

        String template = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0A0A0A; color: #F1F5F9; margin: 0; padding: 20px; }
                    .card { max-width: 600px; margin: 0 auto; background-color: #111827; border: 1px solid #1F2937; border-top: 4px solid #F59E0B; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.5); }
                    .content { padding: 30px 24px; }
                    .plate-container { display: inline-block; background-color: #FBBF24; color: #000000; font-family: monospace, sans-serif; font-size: 24px; font-weight: 900; letter-spacing: 4px; padding: 6px 22px; border-radius: 8px; border: 2px solid #000000; box-shadow: 0 4px 10px rgba(245, 158, 11, 0.3); margin: 15px 0; }
                    .alert-badge { display: inline-block; background-color: {{COLOR_BADGE}}; color: #FFFFFF; font-size: 12px; font-weight: 800; padding: 6px 16px; border-radius: 20px; margin-bottom: 10px; text-transform: uppercase; letter-spacing: 0.5px; }
                    .info-box { background-color: #1E293B; border: 1px solid #334155; border-radius: 12px; padding: 18px; margin: 20px 0; }
                    .info-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #334155; font-size: 14px; }
                    .info-row:last-child { border-bottom: none; }
                    .btn-action { display: inline-block; background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%); color: #000000 !important; font-weight: 800; font-size: 15px; text-decoration: none; padding: 14px 28px; border-radius: 10px; margin-top: 15px; box-shadow: 0 4px 15px rgba(245, 158, 11, 0.4); text-transform: uppercase; letter-spacing: 0.5px; }
                </style>
            </head>
            <body>
                <div class="card">
                    {{HEADER}}
                    <div class="content">
                        <div style="text-align: center;">
                            <span class="alert-badge">{{BADGE_DIAS}}</span>
                            <br />
                            <div class="plate-container">{{PLACA}}</div>
                        </div>
                        <h2 style="color: #FFFFFF; font-size: 18px; margin-top: 10px; text-align: center;">Estimado(a) {{NOMBRE}}</h2>
                        <p style="color: #CBD5E1; font-size: 14px; line-height: 1.6; text-align: center;">
                            Te recordamos que la <strong>Revisión Técnico-Mecánica y de Emisiones Contaminantes</strong> de tu vehículo está próxima a su fecha límite legal.
                        </p>
                        <div class="info-box">
                            <div class="info-row">
                                <span style="color: #94A3B8;">Vehículo:</span>
                                <strong style="color: #FFFFFF;">{{PLACA}} {{CATEGORIA}}</strong>
                            </div>
                            <div class="info-row">
                                <span style="color: #94A3B8;">Fecha Límite:</span>
                                <strong style="color: #F59E0B;">{{FECHA_VENCIMIENTO}}</strong>
                            </div>
                            <div class="info-row">
                                <span style="color: #94A3B8;">Tiempo Restante:</span>
                                <strong style="color: {{COLOR_BADGE}};">{{DIAS_RESTANTES}} días</strong>
                            </div>
                        </div>
                        <div style="text-align: center; margin-top: 25px;">
                            <p style="font-size: 13px; color: #94A3B8; margin-bottom: 12px;">Evita comparendos e inmovilizaciones de tránsito. ¡Visítanos sin filas!</p>
                            <a href="https://wa.me/573113456789?text=Hola%20CDA%20San%20Pedro,%20deseo%20agendar%20mi%20revisión%20técnico-mecánica" class="btn-action">🚗 Agendar Revisión en CDA San Pedro</a>
                        </div>
                    </div>
                    {{FOOTER}}
                </div>
            </body>
            </html>
            """;

        return template
                .replace("{{HEADER}}", getHeaderHtml("Recordatorio Preventivo de RTM"))
                .replace("{{FOOTER}}", getFooterHtml())
                .replace("{{COLOR_BADGE}}", colorBadge)
                .replace("{{BADGE_DIAS}}", badgeDias)
                .replace("{{PLACA}}", placaUpper)
                .replace("{{NOMBRE}}", nombre)
                .replace("{{CATEGORIA}}", cat)
                .replace("{{FECHA_VENCIMIENTO}}", fechaStr)
                .replace("{{DIAS_RESTANTES}}", String.valueOf(diasRestantes));
    }

    /**
     * 2. Plantilla: Comprobante de Pago y Facturación Electrónica DIAN
     */
    public static String buildComprobantePago(String nombreCliente, String placa, String numeroFactura, BigDecimal total, String metodoPago, String cufe, String pdfUrl) {
        String totalFormateado = total != null ? CURRENCY_COP.format(total) : "";
        String nombre = (nombreCliente != null && !nombreCliente.isBlank()) ? nombreCliente : "";
        String placaUpper = (placa != null && !placa.isBlank()) ? placa.toUpperCase() : "";
        String factura = (numeroFactura != null && !numeroFactura.isBlank()) ? numeroFactura : "";
        String metodo = (metodoPago != null && !metodoPago.isBlank()) ? metodoPago : "";

        // Sección Dinámica CUFE
        StringBuilder cufeHtml = new StringBuilder();
        if (cufe != null && !cufe.isBlank() && !cufe.equalsIgnoreCase("N/A")) {
            cufeHtml.append("""
                <div style="background-color: #0F172A; border-left: 3px solid #F59E0B; padding: 12px 16px; border-radius: 6px; font-size: 11px; color: #94A3B8; word-break: break-all; margin-bottom: 16px;">
                    <strong style="color: #F59E0B;">CUFE DIAN:</strong>
                    <div style="margin-top: 4px; font-family: monospace; color: #CBD5E1;">""").append(cufe.trim()).append("""
                    </div>
                </div>
                """);
        }

        // Sección Dinámica de Botones Online
        StringBuilder buttonsHtml = new StringBuilder();
        boolean hasSiigoUrl = pdfUrl != null && !pdfUrl.isBlank() && !pdfUrl.equals("#") && !pdfUrl.equalsIgnoreCase("N/A");
        boolean hasCufe = cufe != null && !cufe.isBlank() && !cufe.equalsIgnoreCase("N/A");

        if (hasSiigoUrl || hasCufe) {
            buttonsHtml.append("<div style=\"text-align: center; margin-top: 10px;\">");
            if (hasSiigoUrl) {
                buttonsHtml.append("""
                    <a href=\"""").append(pdfUrl.trim()).append("""
                    " class="btn-siigo" target="_blank">🌐 Ver Factura Online (SIIGO Cloud)</a>
                    """);
            }
            if (hasCufe) {
                String dianCatalogUrl = "https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey=" + cufe.trim();
                buttonsHtml.append("""
                    <div style="margin-top: 10px;">
                        <a href=\"""").append(dianCatalogUrl).append("""
                        " class="btn-dian" target="_blank">🏛️ Consultar en Catálogo Oficial DIAN</a>
                    </div>
                    """);
            }
            buttonsHtml.append("</div>");
        }

        String template = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0A0A0A; color: #F1F5F9; margin: 0; padding: 20px; }
                    .card { max-width: 600px; margin: 0 auto; background-color: #111827; border: 1px solid #1F2937; border-top: 4px solid #F59E0B; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.5); }
                    .content { padding: 30px 24px; }
                    .receipt-box { background-color: #1E293B; border: 1px solid #334155; border-radius: 12px; padding: 20px; margin: 20px 0; }
                    .receipt-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #334155; font-size: 14px; }
                    .receipt-row:last-child { border-bottom: none; }
                    .total-row { display: flex; justify-content: space-between; padding: 12px 0 0 0; font-size: 18px; font-weight: 800; color: #F59E0B; }
                    .btn-siigo { display: inline-block; background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%); color: #000000 !important; font-weight: 800; font-size: 14px; text-decoration: none; padding: 14px 28px; border-radius: 10px; margin-top: 10px; box-shadow: 0 4px 15px rgba(245, 158, 11, 0.4); text-transform: uppercase; letter-spacing: 0.5px; }
                    .btn-dian { display: inline-block; background-color: #1E293B; border: 1px solid #F59E0B; color: #F59E0B !important; font-weight: 700; font-size: 12px; text-decoration: none; padding: 10px 20px; border-radius: 8px; margin-top: 10px; }
                </style>
            </head>
            <body>
                <div class="card">
                    {{HEADER}}
                    <div class="content">
                        <div style="text-align: center; margin-bottom: 15px;">
                            <span style="display: inline-block; background-color: #10B981; color: #FFFFFF; font-size: 12px; font-weight: 800; padding: 6px 16px; border-radius: 20px; text-transform: uppercase;">
                                ✅ PAGO CONFIRMADO & VALIDADO
                            </span>
                        </div>
                        <h2 style="color: #FFFFFF; font-size: 18px; margin-top: 0; text-align: center;">¡Gracias por tu pago{{NOMBRE_GREETING}}!</h2>
                        <p style="color: #CBD5E1; font-size: 14px; line-height: 1.6; text-align: center;">
                            Hemos recibido satisfactoriamente el pago de la Revisión Técnico-Mecánica para el vehículo {{PLACA_TEXT}}.
                        </p>
                        <div class="receipt-box">
                            <div class="receipt-row">
                                <span style="color: #94A3B8;">Factura / Comprobante:</span>
                                <strong style="color: #F59E0B;">{{FACTURA}}</strong>
                            </div>
                            <div class="receipt-row">
                                <span style="color: #94A3B8;">Vehículo Placa:</span>
                                <strong style="color: #FFFFFF;">{{PLACA}}</strong>
                            </div>
                            <div class="receipt-row">
                                <span style="color: #94A3B8;">Forma de Pago:</span>
                                <strong style="color: #FFFFFF;">{{METODO}}</strong>
                            </div>
                            <div class="total-row">
                                <span>TOTAL PAGADO:</span>
                                <span>{{TOTAL}}</span>
                            </div>
                        </div>
                        
                        {{CUFE_SECTION}}
                        
                        <div style="background-color: #1E293B; border: 1px solid #334155; border-radius: 8px; padding: 12px 16px; margin-bottom: 20px; font-size: 12px; color: #E2E8F0; text-align: left;">
                            📎 <strong>Documento Adjunto:</strong> Hemos adjuntado a este correo el archivo oficial en formato <strong>PDF</strong> emitido conforme a las directrices de facturación.
                        </div>

                        {{BUTTONS_SECTION}}
                    </div>
                    {{FOOTER}}
                </div>
            </body>
            </html>
            """;

        String greeting = !nombre.isBlank() ? ", " + nombre : "";
        String placaText = !placaUpper.isBlank() ? "con placa <strong>" + placaUpper + "</strong>" : "";

        return template
                .replace("{{HEADER}}", getHeaderHtml("Comprobante de Pago & Factura"))
                .replace("{{FOOTER}}", getFooterHtml())
                .replace("{{NOMBRE_GREETING}}", greeting)
                .replace("{{PLACA_TEXT}}", placaText)
                .replace("{{NOMBRE}}", nombre)
                .replace("{{PLACA}}", placaUpper)
                .replace("{{FACTURA}}", factura)
                .replace("{{METODO}}", metodo)
                .replace("{{TOTAL}}", totalFormateado)
                .replace("{{CUFE_SECTION}}", cufeHtml.toString())
                .replace("{{BUTTONS_SECTION}}", buttonsHtml.toString());
    }

    /**
     * 3. Plantilla: Felicitación de Cumpleaños Oficial CDA San Pedro
     */
    public static String buildCumpleanos(String nombreCliente, String mensajeOpcional) {
        String nombre = nombreCliente != null && !nombreCliente.isBlank() ? nombreCliente : "";
        String mensaje = mensajeOpcional != null && !mensajeOpcional.isBlank()
                ? mensajeOpcional
                : "Hoy celebramos tu vida y queremos desearte un año extraordinario, lleno de salud, prosperidad y muchos kilómetros de viajes seguros en compañía de tus seres queridos.";

        String titleGreeting = !nombre.isBlank() ? "¡FELIZ CUMPLEAÑOS, " + nombre.toUpperCase() + "!" : "¡FELIZ CUMPLEAÑOS!";

        String template = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0A0A0A; color: #F1F5F9; margin: 0; padding: 20px; }
                    .card { max-width: 600px; margin: 0 auto; background-color: #111827; border: 1px solid #1F2937; border-top: 4px solid #F59E0B; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.5); }
                    .content { padding: 32px 24px; text-align: center; }
                    .greeting-box { background: linear-gradient(135deg, #1E293B 0%, #0F172A 100%); border: 1px solid #F59E0B; border-radius: 14px; padding: 24px 20px; margin: 25px 0; box-shadow: 0 4px 15px rgba(245, 158, 11, 0.15); }
                </style>
            </head>
            <body>
                <div class="card">
                    {{HEADER}}
                    <div class="content">
                        <div style="font-size: 40px; margin-bottom: 12px;">🎂✨</div>
                        <h2 style="color: #F59E0B; font-size: 22px; margin: 0 0 10px 0; font-weight: 900;">{{TITLE_GREETING}}</h2>
                        <div class="greeting-box">
                            <p style="color: #F8FAFC; font-size: 15px; margin: 0; line-height: 1.7;">
                                {{MENSAJE}}
                            </p>
                        </div>
                        <p style="color: #94A3B8; font-size: 13px; line-height: 1.6; margin: 20px 0 0 0;">
                            Gracias por ser parte de nuestra gran familia. En <strong>CDA San Pedro</strong> estamos siempre comprometidos con tu tranquilidad y seguridad vial.
                        </p>
                        <div style="margin-top: 25px; padding-top: 15px; border-top: 1px solid #1E293B;">
                            <p style="color: #E2E8F0; font-size: 13px; font-weight: 700; margin: 0;">
                                Con todo nuestro aprecio,
                            </p>
                            <p style="color: #F59E0B; font-size: 12px; font-weight: 800; margin: 4px 0 0 0; text-transform: uppercase;">
                                Equipo Directivo & Técnico de CDA San Pedro
                            </p>
                        </div>
                    </div>
                    {{FOOTER}}
                </div>
            </body>
            </html>
            """;

        return template
                .replace("{{HEADER}}", getHeaderHtml("Felicitación Especial de Cumpleaños"))
                .replace("{{FOOTER}}", getFooterHtml())
                .replace("{{TITLE_GREETING}}", titleGreeting)
                .replace("{{NOMBRE}}", nombre)
                .replace("{{MENSAJE}}", mensaje);
    }

    /**
     * 4. Plantilla: Inspección Técnica Finalizada / Vehículo Aprobado
     */
    public static String buildInspeccionAprobada(String nombreCliente, String placa, String certificadoRurt, String fechaInspeccion) {
        String nombre = nombreCliente != null && !nombreCliente.isBlank() ? nombreCliente : "";
        String placaUpper = placa != null && !placa.isBlank() ? placa.toUpperCase() : "";
        String cert = certificadoRurt != null && !certificadoRurt.isBlank() ? certificadoRurt : "";
        String fecha = fechaInspeccion != null && !fechaInspeccion.isBlank() ? fechaInspeccion : LocalDate.now().format(DATE_FORMATTER);

        String template = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0A0A0A; color: #F1F5F9; margin: 0; padding: 20px; }
                    .card { max-width: 600px; margin: 0 auto; background-color: #111827; border: 1px solid #1F2937; border-top: 4px solid #F59E0B; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.5); }
                    .content { padding: 30px 24px; }
                    .plate-box { display: inline-block; background-color: #FBBF24; color: #000000; font-family: monospace, sans-serif; font-size: 24px; font-weight: 900; letter-spacing: 4px; padding: 6px 22px; border-radius: 8px; border: 2px solid #000000; box-shadow: 0 4px 10px rgba(245, 158, 11, 0.3); margin: 10px 0; }
                    .status-approved { background-color: rgba(16, 185, 129, 0.15); border: 1px solid #10B981; color: #34D399; font-weight: 800; font-size: 14px; padding: 12px 16px; border-radius: 10px; text-align: center; margin: 15px 0; }
                </style>
            </head>
            <body>
                <div class="card">
                    {{HEADER}}
                    <div class="content">
                        <div style="text-align: center;">
                            <div class="plate-box">{{PLACA}}</div>
                        </div>
                        <div class="status-approved">
                            ✅ RESULTADO: APROBADO PARA CIRCULACIÓN
                        </div>
                        <h2 style="color: #FFFFFF; font-size: 17px; margin-top: 15px;">Estimado(a) {{NOMBRE}}</h2>
                        <p style="color: #CBD5E1; font-size: 14px; line-height: 1.6;">
                            Nos complace informarte que la prueba técnico-mecánica de tu automotor ha finalizado exitosamente. La información ya ha sido transmitida y validada ante el <strong>RUNT y SICOV</strong>.
                        </p>
                        <div style="background-color: #1E293B; border-radius: 12px; border: 1px solid #334155; padding: 16px; margin: 20px 0; font-size: 13px;">
                            <p style="margin: 0 0 8px 0; color: #94A3B8;">Certificado RURT / Control: <strong style="color: #F59E0B;">{{CERTIFICADO}}</strong></p>
                            <p style="margin: 0; color: #94A3B8;">Fecha de Expedición: <strong style="color: #F8FAFC;">{{FECHA}}</strong></p>
                        </div>
                        <p style="color: #94A3B8; font-size: 13px; line-height: 1.5;">
                            Ya puedes acercarte a la bahía de entrega de vehículos para recibir tus llaves y el certificado físico o digital.
                        </p>
                    </div>
                    {{FOOTER}}
                </div>
            </body>
            </html>
            """;

        return template
                .replace("{{HEADER}}", getHeaderHtml("Inspección Técnica Finalizada"))
                .replace("{{FOOTER}}", getFooterHtml())
                .replace("{{PLACA}}", placaUpper)
                .replace("{{NOMBRE}}", nombre)
                .replace("{{CERTIFICADO}}", cert)
                .replace("{{FECHA}}", fecha);
    }
}
