package com.barterplatform.web.mail;

import com.barterplatform.application.identity.service.MailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * Production SMTP implementation of {@link MailSender} backed by Spring's {@link JavaMailSender}.
 * Registered as a bean only when {@code spring.mail.host} is configured (see {@link MailConfiguration}).
 */
public class SmtpMailSender implements MailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailSender.class);

    private final JavaMailSender javaMailSender;
    private final String from;

    public SmtpMailSender(JavaMailSender javaMailSender, String from) {
        this.javaMailSender = javaMailSender;
        this.from = from;
    }

    @Override
    public void send(String to, String subject, String body) {
        doSend(to, subject, body, false);
    }

    @Override
    public void sendHtml(String to, String subject, String htmlBody) {
        doSend(to, subject, htmlBody, true);
    }

    private void doSend(String to, String subject, String body, boolean html) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            // multipart=false is fine for plain/HTML-only emails (no attachments)
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, html);
            javaMailSender.send(message);
            log.debug("Email sent to {} – subject: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to build email for {} – {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to build email message", e);
        } catch (MailException e) {
            log.error("Failed to send email to {} – {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}

