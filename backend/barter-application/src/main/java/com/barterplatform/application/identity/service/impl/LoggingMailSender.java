package com.barterplatform.application.identity.service.impl;

import com.barterplatform.application.identity.service.MailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development / local mail sender that logs emails instead of sending them.
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
        log.info("  Body:    {}", body);
        log.info("═══════════════════════════════════════════════════");
    }

    @Override
    public void sendHtml(String to, String subject, String htmlBody) {
        // Strip tags for readable log output
        String plainText = htmlBody.replaceAll("<[^>]+>", "").replaceAll("\\s{2,}", " ").strip();
        log.info("═══════════════════════════════════════════════════");
        log.info("  MOCK EMAIL (HTML)");
        log.info("  To:      {}", to);
        log.info("  Subject: {}", subject);
        log.info("  Body:    {}", plainText);
        log.info("═══════════════════════════════════════════════════");
    }
}
