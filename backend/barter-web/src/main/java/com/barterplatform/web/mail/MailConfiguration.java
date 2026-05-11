package com.barterplatform.web.mail;

import com.barterplatform.application.identity.service.MailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Registers the real {@link SmtpMailSender} bean only when:
 * <ol>
 *   <li>{@code spring-boot-starter-mail} is on the classpath ({@link JavaMailSender} present), AND</li>
 *   <li>{@code spring.mail.host} is explicitly configured.</li>
 * </ol>
 *
 * <p>The {@code @ConditionalOnClass} guard prevents Spring from introspecting
 * this class (and hitting a {@link NoClassDefFoundError}) on any classpath that
 * does not include the mail starter — e.g. a stripped-down test configuration.</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(JavaMailSender.class)
@ConditionalOnProperty(name = "spring.mail.host")
public class MailConfiguration {

    @Bean
    public MailSender smtpMailSender(
            JavaMailSender javaMailSender,
            @Value("${barter.mail.from}") String from) {
        return new SmtpMailSender(javaMailSender, from);
    }
}
