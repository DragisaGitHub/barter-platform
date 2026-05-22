package com.barterplatform.web.observability;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.web.security.SecurityConfig;
import com.barterplatform.web.security.jwt.JwtAuthenticationFilter;
import com.barterplatform.web.security.jwt.JwtAuthenticationService;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        classes = ActuatorHealthMvcTest.TestApplication.class,
        properties = "server.servlet.context-path="
)
@AutoConfigureMockMvc
class ActuatorHealthMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SwitchableDbHealthIndicator dbHealthIndicator;

    @BeforeEach
    void setUp() {
        dbHealthIndicator.up();
    }

    @Test
    void shouldExposeApplicationHealthWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER));
    }

    @Test
    void shouldExposeReadinessWhenDatabaseHealthIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void shouldReturnServiceUnavailableWhenDatabaseHealthIsDown() throws Exception {
        dbHealthIndicator.down();

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
    }

    @Test
    void shouldKeepNonHealthActuatorEndpointsProtected() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            DataJpaRepositoriesAutoConfiguration.class
    })
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class})
    static class TestApplication {

        @Bean
        JwtAuthenticationService jwtAuthenticationService() {
            return mock(JwtAuthenticationService.class);
        }

        @Bean
        SwitchableDbHealthIndicator dbHealthIndicator() {
            return new SwitchableDbHealthIndicator();
        }

        /**
         * Spring Boot 4 no longer auto-registers SecurityMockMvcConfigurer with @AutoConfigureMockMvc.
         * This bean ensures @WithMockUser wires the security context into MockMvc correctly.
         */
        @Bean
        MockMvcBuilderCustomizer securityMockMvcCustomizer() {
            return builder -> builder.apply(springSecurity());
        }
    }

    static class SwitchableDbHealthIndicator implements HealthIndicator {

        private final AtomicReference<Status> status = new AtomicReference<>(Status.UP);

        @Override
        public Health health() {
            if (Status.UP.equals(status.get())) {
                return Health.up().build();
            }
            return Health.down().build();
        }

        void up() {
            status.set(Status.UP);
        }

        void down() {
            status.set(Status.DOWN);
        }
    }
}

