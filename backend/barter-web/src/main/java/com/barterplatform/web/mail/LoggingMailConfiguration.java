package com.barterplatform.web.mail;

import com.barterplatform.application.identity.service.MailSender;
import com.barterplatform.application.identity.service.impl.LoggingMailSender;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration fallback: registers {@link LoggingMailSender} only when no
 * other {@link MailSender} bean has been contributed by a user-defined configuration
 * (e.g. {@link MailConfiguration} when {@code spring.mail.host} is set).
 *
 * <p>As an {@code @AutoConfiguration}, this class is processed <em>after</em> all
 * component-scanned {@code @Configuration} classes, which makes
 * {@link ConditionalOnMissingBean} reliable — it will correctly see the
 * {@code smtpMailSender} bean if it was already registered.</p>
 */
@AutoConfiguration
public class LoggingMailConfiguration {

    @Bean
    @ConditionalOnMissingBean(MailSender.class)
    public MailSender loggingMailSender() {
        return new LoggingMailSender();
    }
}
