package com.barterplatform.web.identity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barterplatform.BarterApplication;
import com.barterplatform.domain.identity.entity.RoleEntity;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.identity.entity.UserRoleEntity;
import com.barterplatform.domain.identity.enums.PreferredLanguage;
import com.barterplatform.domain.identity.enums.RoleCode;
import com.barterplatform.infrastructure.identity.repository.EmailVerificationCodeRepository;
import com.barterplatform.infrastructure.identity.repository.RoleRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.identity.repository.UserRoleRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
        classes = BarterApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration"
        }
)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AuthRegistrationIntegrationTest {

    @SuppressWarnings("resource")
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("barter_db")
            .withUsername("barter_user")
            .withPassword("barter_password");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EmailVerificationCodeRepository emailVerificationCodeRepository;

    @BeforeEach
    void cleanMutableTables() {
        emailVerificationCodeRepository.deleteAllInBatch();
        userRoleRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void shouldRegisterUserEndToEnd() throws Exception {
        String rawPassword = "P@ssword123";

        MvcResult result = mockMvc.perform(registerUserRequest("alex99", "alex@example.com", rawPassword))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").isNotEmpty())
                .andExpect(jsonPath("$.username").value("alex99"))
                .andExpect(jsonPath("$.email").value("alex@example.com"))
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.preferredLanguage").value("SR"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("passwordHash");
        assertThat(result.getResponse().getContentAsString()).doesNotContain(rawPassword);

        UserEntity savedUser = userRepository.findByEmail("alex@example.com").orElseThrow();
        assertThat(savedUser.getUuid()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("alex99");
        assertThat(savedUser.getEmail()).isEqualTo("alex@example.com");
        assertThat(savedUser.getPasswordHash()).isNotBlank();
        assertThat(savedUser.getPasswordHash()).isNotEqualTo(rawPassword);
        assertThat(savedUser.getPreferredLanguage()).isEqualTo(PreferredLanguage.SR);
        assertThat(savedUser.getStatus()).isEqualTo(com.barterplatform.domain.identity.enums.UserStatus.PENDING_VERIFICATION);
        assertThat(savedUser.isEmailVerified()).isFalse();
        assertThat(emailVerificationCodeRepository.findAll())
                .singleElement()
                .satisfies(code -> {
                    assertThat(code.getUserId()).isEqualTo(savedUser.getId());
                    assertThat(code.getCodeHash()).isNotBlank();
                    assertThat(code.getExpiresAt()).isNotNull();
                    assertThat(code.getUsedAt()).isNull();
                });

        RoleEntity userRole = roleRepository.findByCode(RoleCode.USER).orElseThrow();
        List<UserRoleEntity> roleAssignments = userRoleRepository.findAllByIdUserIdOrderByAssignedAtAsc(savedUser.getId());
        assertThat(roleAssignments)
                .hasSize(1)
                .first()
                .satisfies(assignment -> {
                    assertThat(assignment.getId().getUserId()).isEqualTo(savedUser.getId());
                    assertThat(assignment.getId().getRoleId()).isEqualTo(userRole.getId());
                    assertThat(assignment.getAssignedAt()).isNotNull();
                });
    }

    @Test
    void shouldReturnConflictForDuplicateEmail() throws Exception {
        mockMvc.perform(registerUserRequest("alex99", "alex@example.com", "P@ssword123"))
                .andExpect(status().isCreated());

        mockMvc.perform(registerUserRequest("alex-second", "alex@example.com", "P@ssword456"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Email 'alex@example.com' is already in use."));
    }

    @Test
    void shouldReturnConflictForDuplicateEmailIgnoringCase() throws Exception {
        mockMvc.perform(registerUserRequest("dragisa", "Dragisa@Example.com", "P@ssword123"))
                .andExpect(status().isCreated());

        mockMvc.perform(registerUserRequest("dragisa-second", "dragisa@example.com", "P@ssword456"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Email 'dragisa@example.com' is already in use."));
    }

    @Test
    void shouldReturnConflictForDuplicateUsername() throws Exception {
        mockMvc.perform(registerUserRequest("alex99", "alex@example.com", "P@ssword123"))
                .andExpect(status().isCreated());

        mockMvc.perform(registerUserRequest("alex99", "alex2@example.com", "P@ssword456"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Username 'alex99' is already in use."));
    }

    @Test
    void shouldReturnConflictForDuplicateUsernameIgnoringCase() throws Exception {
        mockMvc.perform(registerUserRequest("Dragisa", "dragisa@example.com", "P@ssword123"))
                .andExpect(status().isCreated());

        mockMvc.perform(registerUserRequest("dragisa", "dragisa2@example.com", "P@ssword456"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Username 'dragisa' is already in use."));
    }

    private MockHttpServletRequestBuilder registerUserRequest(String username, String email, String password) {
        return post("/api/v1/auth/register")
                .contextPath("/api/v1")
                .servletPath("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerRequestJson(username, email, password));
    }

    private String registerRequestJson(String username, String email, String password) {
        return """
                {
                  "username": "%s",
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(username, email, password);
    }
}

