package com.cdasanpedro.core.gateway;

import java.util.Map;

public interface NotificationGateway {
    
    /**
     * Envía una notificación transaccional por correo electrónico.
     */
    void sendEmail(String to, String subject, String bodyHtml, byte[] attachmentPdf, String attachmentName);

    /**
     * Envía un mensaje transaccional por WhatsApp.
     */
    void sendWhatsAppMessage(String phoneNumber, String templateOrMessage, Map<String, String> parameters);
}
