package com.barterplatform.application.identity.service;

/**
 * Abstraction for sending emails. Implementations can be a real SMTP sender
 * or a local/dev logging sender.
 */
public interface MailSender {

    /**
     * Send a plain-text email.
     *
     * @param to      recipient email address
     * @param subject email subject
     * @param body    plain-text email body
     */
    void send(String to, String subject, String body);

    /**
     * Send an HTML email.
     *
     * @param to       recipient email address
     * @param subject  email subject
     * @param htmlBody HTML email body
     */
    void sendHtml(String to, String subject, String htmlBody);
}

