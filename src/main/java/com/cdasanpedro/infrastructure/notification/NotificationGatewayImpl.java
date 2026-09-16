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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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

    @org.springframework.beans.factory.annotation.Value("${app.sms.android-gateway-url:${ANDROID_SMS_GATEWAY_URL:}}")
    private String androidSmsGatewayUrl;

    @org.springframework.beans.factory.annotation.Value("${app.sms.android-gateway-token:${ANDROID_SMS_GATEWAY_TOKEN:}}")
    private String androidSmsGatewayToken;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

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
                    ClassPathResource logoResource = new ClassPathResource("assets/LOGOCDAOPT.PNG");
                    if (!logoResource.exists()) {
                        logoResource = new ClassPathResource("assets/LogoCDA.PNG");
                    }
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
        String header = EmailTemplateBuilder.getHeaderHtml(titulo != null ? titulo : "Notificación Oficial");
        String footer = EmailTemplateBuilder.getFooterHtml();
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0A0A0A; color: #F1F5F9; margin: 0; padding: 20px; }
                    .card { max-width: 600px; margin: 0 auto; background-color: #111827; border: 1px solid #1F2937; border-top: 4px solid #F59E0B; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.5); }
                    .body { padding: 30px; font-size: 15px; line-height: 1.6; color: #CBD5E1; }
                    .highlight-box { background-color: #1E293B; border-left: 4px solid #F59E0B; padding: 18px; border-radius: 8px; margin: 20px 0; color: #F8FAFC; }
                </style>
            </head>
            <body>
                <div class="card">
                    {{HEADER}}
                    <div class="body">
                        <h2 style="color: #FFFFFF; font-size: 18px; margin-top: 0;">{{TITULO}}</h2>
                        <div class="highlight-box">
                            {{CONTENIDO}}
                        </div>
                        <p style="font-size: 13px; color: #94A3B8;">Si tienes alguna pregunta o requieres soporte, comunícate con nuestras líneas de atención autorizadas.</p>
                    </div>
                    {{FOOTER}}
                </div>
            </body>
            </html>
            """
            .replace("{{HEADER}}", header)
            .replace("{{FOOTER}}", footer)
            .replace("{{TITULO}}", titulo != null ? titulo : "")
            .replace("{{CONTENIDO}}", contenido != null ? contenido : "");
    }

    @Override
    @Async
    public void sendWhatsAppMessage(String phoneNumber, String templateOrMessage, Map<String, String> parameters) {
        log.info("[NotificationGateway - SMS/Móvil] Procesando despacho a celular: {}", phoneNumber);

        // Si está configurada la URL de Android SMS Gateway (servidor local gratuito por app en celular)
        if (androidSmsGatewayUrl != null && !androidSmsGatewayUrl.isBlank()) {
            try {
                // Formato JSON universal para apps Android SMS Gateway
                String jsonBody = String.format(
                        "{\"phone\":\"%s\",\"to\":\"%s\",\"message\":\"%s\",\"text\":\"%s\"}",
                        phoneNumber,
                        phoneNumber,
                        escapeJson(templateOrMessage),
                        escapeJson(templateOrMessage)
                );

                var requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(androidSmsGatewayUrl.trim()))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json");

                if (androidSmsGatewayToken != null && !androidSmsGatewayToken.isBlank()) {
                    requestBuilder.header("Authorization", "Bearer " + androidSmsGatewayToken.trim());
                    requestBuilder.header("X-API-Key", androidSmsGatewayToken.trim());
                }

                HttpRequest request = requestBuilder
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    log.info("[Android SMS Gateway] SMS despachado exitosamente a {} vía celular Android (Status: {})", phoneNumber, response.statusCode());
                } else {
                    log.warn("[Android SMS Gateway] Respuesta no exitosa del móvil (Status: {}): {}", response.statusCode(), response.body());
                }
            } catch (Exception ex) {
                log.error("[Android SMS Gateway] Error al conectar con el celular Android {}: {}", androidSmsGatewayUrl, ex.getMessage());
            }
        } else {
            log.info("[NotificationGateway - SMS Móvil SIMULADO] Celular: {} - Mensaje: {} (Para enviar SMS reales gratis mediante un celular Android, configure ANDROID_SMS_GATEWAY_URL en .env)",
                    phoneNumber, templateOrMessage);
        }
    }

    private String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
