package com.developteca.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@developteca.com}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender){
        this.mailSender = mailSender;
    }

    // =============== ENVIAR EMAIL DE VERIFICACIÓN ==============
    public void sendVerificationEmail(String toEmail, String token, String firstName){
        try{
            String verificationLink = "http://localhost:4200/verify-email?token=" + token;

            String subject = "Verifica tu email en Developteca";
            String body =  "Hola " + firstName + ",\n\n" +
                    "Gracias por registrarte en Developteca.\n\n" +
                    "Para completar tu registro, verifica tu email haciendo click en el siguiente enlace:\n\n" +
                    verificationLink + "\n\n" +
                    "Este enlace expira en 24 horas.\n\n" +
                    "Si no solicitaste este registro, ignora este email.\n\n" +
                    "Saludos,\nEl equipo de Developteca";
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
        }catch(Exception e){
            System.err.println("Error enviando mail: " + e.getMessage());
        }
    }

    // ============= ENVIAR EMAIL DE CONTRASEÑA RECUPERADA =============
    public void sendPasswordResetEmail(String toEmail, String token, String firstName) {
        try {
            String resetLink = "http://localhost:4200/reset-password?token=" + token;
            
            String subject = "Recuperar contraseña - Developteca";
            String body = "Hola " + firstName + ",\n\n" +
                    "Recibimos una solicitud para recuperar tu contraseña.\n\n" +
                    "Haz click en el siguiente enlace para establecer una nueva contraseña:\n\n" +
                    resetLink + "\n\n" +
                    "Este enlace expira en 1 hora.\n\n" +
                    "Si no solicitaste esto, ignora este email.\n\n" +
                    "Saludos,\nEl equipo de Developteca";

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error enviando email: " + e.getMessage());
        }
    }

}
