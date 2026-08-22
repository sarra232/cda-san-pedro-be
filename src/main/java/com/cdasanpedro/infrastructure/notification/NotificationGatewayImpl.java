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

import java.util.Map;

@Service
public class NotificationGatewayImpl implements NotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(NotificationGatewayImpl.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Override
    @Async
    public void sendEmail(String to, String subject, String bodyHtml, byte[] attachmentPdf, String attachmentName) {
        log.info("[NotificationGateway] Preparando envío de correo a: {} - Asunto: {}", to, subject);
        try {
            if (mailSender != null) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(bodyHtml, true);

                if (attachmentPdf != null && attachmentName != null) {
                    helper.addAttachment(attachmentName, new ByteArrayResource(attachmentPdf));
                }
                mailSender.send(message);
                log.info("[NotificationGateway] Correo enviado exitosamente a {}", to);
            } else {
                log.warn("[NotificationGateway - SIMULACIÓN] JavaMailSender no configurado. Correo simulado a {}", to);
            }
        } catch (Exception e) {
            log.error("[NotificationGateway] Error al despachar correo a {}: {}", to, e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void sendWhatsAppMessage(String phoneNumber, String templateOrMessage, Map<String, String> parameters) {
        // Adaptador agnóstico: actualmente registra en log y deja el hook listo para Meta Cloud API o Evolution API
        log.info("[NotificationGateway - WhatsApp] Encolado mensaje para celular: {} - Mensaje/Template: {} - Parámetros: {}", 
                phoneNumber, templateOrMessage, parameters);
    }
}
