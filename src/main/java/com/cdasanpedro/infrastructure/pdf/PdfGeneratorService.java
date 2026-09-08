package com.cdasanpedro.infrastructure.pdf;

import com.cdasanpedro.infrastructure.persistence.entity.FacturaEntity;
import com.cdasanpedro.infrastructure.persistence.entity.ItemFacturaEntity;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
public class PdfGeneratorService {

    private static final NumberFormat COP_FORMAT = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

    // Paleta de colores oficial CDA San Pedro
    private static final Color COLOR_PRIMARY_DARK = new Color(17, 24, 39);      // #111827
    private static final Color COLOR_AMBER = new Color(217, 119, 6);           // #D97706
    private static final Color COLOR_BG_LIGHT = new Color(248, 250, 252);       // #F8FAFC
    private static final Color COLOR_BORDER = new Color(226, 232, 240);         // #E2E8F0

    public byte[] generarFacturaPdf(FacturaEntity factura) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Encabezado con Logo y Datos de la Empresa
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{1.2f, 2f});

            // Celda 1: Logotipo Oficial
            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            try {
                ClassPathResource logoResource = new ClassPathResource("assets/LogoCDA.PNG");
                if (logoResource.exists()) {
                    try (InputStream is = logoResource.getInputStream()) {
                        byte[] logoBytes = is.readAllBytes();
                        Image logo = Image.getInstance(logoBytes);
                        logo.scaleToFit(140, 60);
                        logoCell.addElement(logo);
                    }
                } else {
                    Paragraph logoText = new Paragraph("CDA SAN PEDRO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, COLOR_AMBER));
                    logoCell.addElement(logoText);
                }
            } catch (Exception e) {
                log.warn("No se pudo cargar el logo en el PDF, usando texto alternativo: {}", e.getMessage());
                Paragraph logoText = new Paragraph("CDA SAN PEDRO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, COLOR_AMBER));
                logoCell.addElement(logoText);
            }
            headerTable.addCell(logoCell);

            // Celda 2: Datos Fiscales de la Empresa
            PdfPCell companyCell = new PdfPCell();
            companyCell.setBorder(Rectangle.NO_BORDER);
            companyCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            Paragraph cName = new Paragraph("CDA SAN PEDRO S.A.S.", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, COLOR_PRIMARY_DARK));
            cName.setAlignment(Element.ALIGN_RIGHT);
            companyCell.addElement(cName);

            Paragraph cNit = new Paragraph("NIT: 901.558.942-1 | Régimen Común", FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY));
            cNit.setAlignment(Element.ALIGN_RIGHT);
            companyCell.addElement(cNit);

            Paragraph cDir = new Paragraph("Cra. 50 # 48-20, San Pedro de los Milagros, Antioquia", FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY));
            cDir.setAlignment(Element.ALIGN_RIGHT);
            companyCell.addElement(cDir);

            Paragraph cTel = new Paragraph("Tel: (604) 868 6060 | Cel/WhatsApp: 311 345 6789", FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY));
            cTel.setAlignment(Element.ALIGN_RIGHT);
            companyCell.addElement(cTel);

            Paragraph cDian = new Paragraph("Aut. DIAN No. 18764000123 de 2026 • Habilita: FAC-1 a FAC-50000", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7, Color.GRAY));
            cDian.setAlignment(Element.ALIGN_RIGHT);
            companyCell.addElement(cDian);

            headerTable.addCell(companyCell);
            document.add(headerTable);

            // Separador
            document.add(new Paragraph(" "));
            LineSeparator sep = new LineSeparator(1f, 100, COLOR_BORDER, Element.ALIGN_CENTER, -2);
            document.add(sep);
            document.add(new Paragraph(" "));

            // 2. Título de Factura y Consecutivo
            PdfPTable titleTable = new PdfPTable(2);
            titleTable.setWidthPercentage(100);
            titleTable.setWidths(new float[]{1.5f, 1.5f});

            PdfPCell invoiceTitleCell = new PdfPCell();
            invoiceTitleCell.setBorder(Rectangle.NO_BORDER);
            Paragraph invTitle = new Paragraph("FACTURA DE VENTA ELECTRÓNICA", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_PRIMARY_DARK));
            Paragraph invNum = new Paragraph(factura.getNumeroFactura(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, COLOR_AMBER));
            invoiceTitleCell.addElement(invTitle);
            invoiceTitleCell.addElement(invNum);
            titleTable.addCell(invoiceTitleCell);

            PdfPCell invoiceMetaCell = new PdfPCell();
            invoiceMetaCell.setBorder(Rectangle.NO_BORDER);
            invoiceMetaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph invDate = new Paragraph("Fecha de Emisión: " + (factura.getFechaEmision() != null ? factura.getFechaEmision().format(DATE_FORMATTER) : ""), FontFactory.getFont(FontFactory.HELVETICA, 9, COLOR_PRIMARY_DARK));
            invDate.setAlignment(Element.ALIGN_RIGHT);
            Paragraph invMetodo = new Paragraph("Método de Pago: " + factura.getMetodoPago().name(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COLOR_PRIMARY_DARK));
            invMetodo.setAlignment(Element.ALIGN_RIGHT);
            Paragraph invCajero = new Paragraph("Atendido por: " + (factura.getUsuario() != null ? factura.getUsuario().getNombresApellidos() : "Ventanilla Central"), FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY));
            invCajero.setAlignment(Element.ALIGN_RIGHT);

            invoiceMetaCell.addElement(invDate);
            invoiceMetaCell.addElement(invMetodo);
            invoiceMetaCell.addElement(invCajero);
            titleTable.addCell(invoiceMetaCell);

            document.add(titleTable);
            document.add(new Paragraph(" "));

            // 3. Cuadros de Datos: Cliente / Pagador y Vehículo Inspeccionado
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{1f, 1f});

            // Bloque Cliente
            PdfPCell clientBox = new PdfPCell();
            clientBox.setBackgroundColor(COLOR_BG_LIGHT);
            clientBox.setBorderColor(COLOR_BORDER);
            clientBox.setPadding(10);

            Paragraph clientHeader = new Paragraph("DATOS DEL CLIENTE / PAGADOR", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COLOR_PRIMARY_DARK));
            clientBox.addElement(clientHeader);
            if (factura.getClienteFactura() != null) {
                clientBox.addElement(new Paragraph("Nombre / Razón: " + factura.getClienteFactura().getNombresRazonSocial(), FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK)));
                clientBox.addElement(new Paragraph("Documento: " + factura.getClienteFactura().getTipoDocumento() + " " + factura.getClienteFactura().getNumeroDocumento(), FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK)));
                clientBox.addElement(new Paragraph("Celular / WhatsApp: " + factura.getClienteFactura().getCelular(), FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK)));
                if (factura.getClienteFactura().getDireccion() != null) {
                    clientBox.addElement(new Paragraph("Dirección: " + factura.getClienteFactura().getDireccion(), FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK)));
                }
            }
            infoTable.addCell(clientBox);

            // Bloque Vehículo
            PdfPCell vehBox = new PdfPCell();
            vehBox.setBackgroundColor(COLOR_BG_LIGHT);
            vehBox.setBorderColor(COLOR_BORDER);
            vehBox.setPadding(10);

            Paragraph vehHeader = new Paragraph("VEHÍCULO INSPECCIONADO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COLOR_PRIMARY_DARK));
            vehBox.addElement(vehHeader);
            if (factura.getOrdenIngreso() != null && factura.getOrdenIngreso().getVehiculo() != null) {
                var veh = factura.getOrdenIngreso().getVehiculo();
                String placaFormateada = formatPlaca(veh.getPlaca());
                vehBox.addElement(new Paragraph("Placa: " + placaFormateada + " (" + veh.getCategoria() + ")", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COLOR_AMBER)));
                vehBox.addElement(new Paragraph("Marca / Línea: " + veh.getMarca() + " " + veh.getLinea() + " Modelo " + veh.getModelo(), FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK)));
                vehBox.addElement(new Paragraph("Kilometraje: " + factura.getOrdenIngreso().getKilometraje() + " km", FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK)));
                vehBox.addElement(new Paragraph("Turno de Recepción: #" + factura.getOrdenIngreso().getConsecutivo(), FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK)));
            }
            infoTable.addCell(vehBox);

            document.add(infoTable);
            document.add(new Paragraph(" "));

            // 4. Tabla de Ítems / Conceptos Facturados
            PdfPTable itemsTable = new PdfPTable(4);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{3f, 0.8f, 1.2f, 1.2f});

            // Encabezados de tabla
            String[] headers = {"Descripción del Concepto", "Cant.", "V. Unitario", "Total"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
                cell.setBackgroundColor(COLOR_PRIMARY_DARK);
                cell.setPadding(6);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBorder(Rectangle.NO_BORDER);
                itemsTable.addCell(cell);
            }

            // Filas de ítems
            if (factura.getItems() != null && !factura.getItems().isEmpty()) {
                for (ItemFacturaEntity item : factura.getItems()) {
                    PdfPCell descCell = new PdfPCell(new Phrase(item.getDescripcion(), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                    descCell.setPadding(6);
                    descCell.setBorderColor(COLOR_BORDER);
                    itemsTable.addCell(descCell);

                    PdfPCell cantCell = new PdfPCell(new Phrase(String.valueOf(item.getCantidad()), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                    cantCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cantCell.setPadding(6);
                    cantCell.setBorderColor(COLOR_BORDER);
                    itemsTable.addCell(cantCell);

                    PdfPCell unitCell = new PdfPCell(new Phrase(formatCop(item.getValorUnitario()), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                    unitCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    unitCell.setPadding(6);
                    unitCell.setBorderColor(COLOR_BORDER);
                    itemsTable.addCell(unitCell);

                    PdfPCell totalCell = new PdfPCell(new Phrase(formatCop(item.getTotalItem()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
                    totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totalCell.setPadding(6);
                    totalCell.setBorderColor(COLOR_BORDER);
                    itemsTable.addCell(totalCell);
                }
            } else {
                // Fila por defecto si no hay lista explícita
                PdfPCell descCell = new PdfPCell(new Phrase("Revisión Técnico-Mecánica y Emisiones Contaminantes (RTM)", FontFactory.getFont(FontFactory.HELVETICA, 8)));
                descCell.setPadding(6);
                descCell.setBorderColor(COLOR_BORDER);
                itemsTable.addCell(descCell);

                PdfPCell cantCell = new PdfPCell(new Phrase("1", FontFactory.getFont(FontFactory.HELVETICA, 8)));
                cantCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cantCell.setPadding(6);
                cantCell.setBorderColor(COLOR_BORDER);
                itemsTable.addCell(cantCell);

                PdfPCell unitCell = new PdfPCell(new Phrase(formatCop(factura.getSubtotal()), FontFactory.getFont(FontFactory.HELVETICA, 8)));
                unitCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                unitCell.setPadding(6);
                unitCell.setBorderColor(COLOR_BORDER);
                itemsTable.addCell(unitCell);

                PdfPCell totalCell = new PdfPCell(new Phrase(formatCop(factura.getSubtotal()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
                totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                totalCell.setPadding(6);
                totalCell.setBorderColor(COLOR_BORDER);
                itemsTable.addCell(totalCell);
            }

            document.add(itemsTable);

            // 5. Bloque de Totales
            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(100);
            totalsTable.setWidths(new float[]{2.5f, 1.5f});

            // Celda izquierda: Nota legal y QR/Firma
            PdfPCell legalNoteCell = new PdfPCell();
            legalNoteCell.setBorder(Rectangle.NO_BORDER);
            legalNoteCell.setPadding(8);
            Paragraph note = new Paragraph("Esta factura de venta es un título valor conforme al Art. 774 del Código de Comercio. El servicio de diagnóstico técnico automotor no garantiza la aprobación ante autoridades viales si el vehículo sufre modificaciones posteriores a la inspección.", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7, Color.GRAY));
            legalNoteCell.addElement(note);
            totalsTable.addCell(legalNoteCell);

            // Celda derecha: Subtotal, IVA, Total
            PdfPCell numbersCell = new PdfPCell();
            numbersCell.setBorder(Rectangle.NO_BORDER);
            numbersCell.setPadding(4);

            PdfPTable calcTable = new PdfPTable(2);
            calcTable.setWidthPercentage(100);
            calcTable.setWidths(new float[]{1.2f, 1.2f});

            addTotalRow(calcTable, "SUBTOTAL:", formatCop(factura.getSubtotal()), false);
            addTotalRow(calcTable, "IVA (19%):", formatCop(factura.getIva()), false);
            addTotalRow(calcTable, "TOTAL PAGADO:", formatCop(factura.getTotal()), true);

            numbersCell.addElement(calcTable);
            totalsTable.addCell(numbersCell);

            document.add(totalsTable);

            // 6. Pie de Página
            document.add(new Paragraph(" "));
            Paragraph footer = new Paragraph("¡Gracias por confiar en CDA San Pedro! • Seguridad y Precisión para tu Vehículo", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, COLOR_PRIMARY_DARK));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generando el comprobante PDF de la factura: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar PDF de factura: " + e.getMessage(), e);
        }
    }

    private void addTotalRow(PdfPTable table, String label, String value, boolean isFinalTotal) {
        Font fontLabel = isFinalTotal 
                ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COLOR_PRIMARY_DARK)
                : FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
        Font fontValue = isFinalTotal 
                ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_AMBER)
                : FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);

        PdfPCell cLabel = new PdfPCell(new Phrase(label, fontLabel));
        cLabel.setBorder(Rectangle.NO_BORDER);
        cLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cLabel.setPadding(3);
        if (isFinalTotal) cLabel.setBackgroundColor(COLOR_BG_LIGHT);
        table.addCell(cLabel);

        PdfPCell cVal = new PdfPCell(new Phrase(value, fontValue));
        cVal.setBorder(Rectangle.NO_BORDER);
        cVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cVal.setPadding(3);
        if (isFinalTotal) cVal.setBackgroundColor(COLOR_BG_LIGHT);
        table.addCell(cVal);
    }

    private String formatCop(BigDecimal amount) {
        if (amount == null) return "$ 0";
        return COP_FORMAT.format(amount).replace(",00", "");
    }

    private String formatPlaca(String placa) {
        if (placa == null || placa.isBlank()) return "";
        String clean = placa.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (clean.length() <= 3) return clean;
        return clean.substring(0, 3) + "-" + clean.substring(3);
    }
}
