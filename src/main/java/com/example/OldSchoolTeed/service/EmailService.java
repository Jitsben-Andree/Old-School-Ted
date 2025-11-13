package com.example.OldSchoolTeed.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * MODIFICADO: Renombrado a 'sendRecoveryCodeEmail' y texto actualizado.
     * Envía un correo con el código de recuperación de contraseña.
     * Ya no se usa para el bloqueo automático.

     * @param to Email del destinatario
     * @param code Código de 6 dígitos
     */
    public void sendRecoveryCodeEmail(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Recupera tu contraseña - OldSchoolTeed");
            message.setText("Has solicitado restablecer tu contraseña.\n\n" +
                    "Tu código de recuperación es: " + code + "\n\n" +
                    "Este código expirará en 15 minutos.\n" +
                    "Si no fuiste tú, puedes ignorar este correo.");
            mailSender.send(message);
        } catch (Exception e) {
            // Loggear el error, pero también lanzarlo para que el servicio superior lo sepa
            System.err.println("Error al enviar email de recuperación: " + e.getMessage());
            throw new RuntimeException("Error al enviar el email: " + e.getMessage(), e);
        }
    }
}