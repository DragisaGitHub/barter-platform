package com.barterplatform.application.identity.service.impl;

import com.barterplatform.application.identity.service.MailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development / local mail sender that logs email metadata without message bodies.
 * Registered as a Spring bean only when no SMTP host is configured (see MailConfiguration).
 */
public class LoggingMailSender implements MailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailSender.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("═══════════════════════════════════════════════════");
        log.info("  MOCK EMAIL");
        log.info("  To:      {}", to);
        log.info("  Subject: {}", subject);
        log.info("  Body:    [omitted]");
        log.info("═══════════════════════════════════════════════════");
    }

    @Override
    public void sendHtml(String to, String subject, String htmlBody) {
        log.info("═══════════════════════════════════════════════════");
        log.info("  MOCK EMAIL (HTML)");
        log.info("  To:      {}", to);
        log.info("  Subject: {}", subject);
        log.info("  Body:    [omitted]");
        log.info("═══════════════════════════════════════════════════");
    }
}
