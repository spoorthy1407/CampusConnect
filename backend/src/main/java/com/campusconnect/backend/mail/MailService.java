package com.campusconnect.backend.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> sender;
    @Value("${app.mail.enabled:false}")
    private boolean enabled;
    @Value("${app.mail.from:}")
    private String from;
    @Value("${spring.mail.username:}")
    private String username;

    public MailService(ObjectProvider<JavaMailSender> sender) { this.sender = sender; }

    @Async
    public void send(String to, String subject, String body) {
        JavaMailSender mailSender = sender.getIfAvailable();
        if (!enabled || mailSender == null) {
            log.info("Email not sent (app.mail.enabled=false). To: {} | Subject: {}\n{}", to, subject, body);
            return;
        }
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            String sendFrom = !from.isBlank() ? from : username;
            if (!sendFrom.isBlank()) m.setFrom(sendFrom);
            m.setTo(to);
            m.setSubject(subject);
            m.setText(body);
            mailSender.send(m);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.warn("Could not send email to {}: {}", to, e.getMessage());
        }
    }
}
