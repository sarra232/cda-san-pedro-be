package com.cdasanpedro.infrastructure.notification;

import com.cdasanpedro.core.gateway.NotificationGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import java.util.Map;

@Service
public class NotificationGatewayImpl implements NotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(NotificationGatewayImpl.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @org.springframework.beans.factory.annotation.Value("${app.mail.from-address:${spring.mail.username:}}")
    private String fromAddress;

    @org.springframework.beans.factory.annotation.Value("${app.mail.from-name:CDA San Pedro}")
    private String fromName;

    @Override
    @Async
    public void sendEmail(String to, String subject, String bodyHtml, byte[] attachmentPdf, String attachmentName) {
        log.info("[NotificationGateway] Preparando envío de correo a: {} - Asunto: {}", to, subject);
        try {
            if (mailSender != null && fromAddress != null && !fromAddress.isBlank()) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                
                helper.setFrom(fromAddress, fromName);
                helper.setTo(to);
                helper.setSubject(subject);

                String formattedHtml = bodyHtml;
                if (bodyHtml != null && !bodyHtml.contains("<html") && !bodyHtml.contains("<div")) {
                    formattedHtml = construirPlantillaHtml(subject, bodyHtml);
                }

                helper.setText(formattedHtml != null ? formattedHtml : "", true);

                // Incrustar logotipo inline vía CID (Content-ID) para visualización nativa sin depender de hosting externo
                try {
                    ClassPathResource logoResource = new ClassPathResource("assets/LogoCDA.PNG");
                    if (logoResource.exists()) {
                        helper.addInline("logoCda", logoResource, "image/png");
                    }
                } catch (Exception exLogo) {
                    log.warn("[NotificationGateway] No se pudo adjuntar logotipo inline: {}", exLogo.getMessage());
                }

                if (attachmentPdf != null && attachmentName != null) {
                    helper.addAttachment(attachmentName, new ByteArrayResource(attachmentPdf));
                }
                mailSender.send(message);
                log.info("[NotificationGateway] Correo enviado exitosamente a {}", to);
            } else {
                log.warn("[NotificationGateway - SIMULACIÓN] JavaMailSender o remitente no configurado (MAIL_USERNAME/MAIL_PASSWORD vacíos). Correo simulado a {}", to);
            }
        } catch (Exception e) {
            log.error("[NotificationGateway] Error al despachar correo a {}: {}", to, e.getMessage(), e);
        }
    }

    private String construirPlantillaHtml(String titulo, String contenido) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0A0A0A; color: #F1F5F9; margin: 0; padding: 20px; }
                    .card { max-width: 600px; margin: 0 auto; background-color: #111827; border: 1px solid #1F2937; border-radius: 16px; overflow: hidden; }
                    .header { background: linear-gradient(135deg, #111827 0%, #1F2937 100%); padding: 24px; border-bottom: 2px solid #F59E0B; text-align: center; }
                    .header h1 { margin: 0; color: #F59E0B; font-size: 20px; font-weight: 800; letter-spacing: 1px; }
                    .body { padding: 30px; font-size: 15px; line-height: 1.6; color: #CBD5E1; }
                    .highlight-box { background-color: #1E293B; border-left: 4px solid #F59E0B; padding: 15px; border-radius: 8px; margin: 20px 0; color: #F8FAFC; }
                    .footer { background-color: #0B0F17; padding: 16px; text-align: center; font-size: 11px; color: #64748B; border-top: 1px solid #1E293B; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="header">
                        <h1>CDA SAN PEDRO</h1>
                        <p style="margin: 4px 0 0 0; font-size: 12px; color: #94A3B8;">Centro de Diagnóstico Automotor Oficial</p>
                    </div>
                    <div class="body">
                        <h2 style="color: #FFFFFF; font-size: 18px; margin-top: 0;">{{TITULO}}</h2>
                        <div class="highlight-box">
                            {{CONTENIDO}}
                        </div>
                        <p style="font-size: 13px; color: #94A3B8;">Si tienes alguna pregunta o requieres soporte, comunícate con nuestras líneas de atención autorizadas.</p>
                    </div>
                    <div class="footer">
                        <p style="margin: 0;">© 2026 CDA San Pedro. Todos los derechos reservados.</p>
                    </div>
                </div>
            </body>
            </html>
            """
            .replace("{{TITULO}}", titulo != null ? titulo : "")
            .replace("{{CONTENIDO}}", contenido != null ? contenido : "");
    }

    @Override
    @Async
    public void sendWhatsAppMessage(String phoneNumber, String templateOrMessage, Map<String, String> parameters) {
        // Adaptador agnóstico: actualmente registra en log y deja el hook listo para Meta Cloud API o Evolution API
        log.info("[NotificationGateway - WhatsApp] Encolado mensaje para celular: {} - Mensaje/Template: {} - Parámetros: {}", 
                phoneNumber, templateOrMessage, parameters);
    }
}
