package com.barterplatform.web.catalog.controller;

import com.barterplatform.BarterApplication;
import com.barterplatform.application.catalog.storage.FileStorageService;
import com.barterplatform.infrastructure.catalog.repository.ItemImageRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemTagRepository;
import com.barterplatform.infrastructure.identity.repository.EmailVerificationCodeRepository;
import com.barterplatform.infrastructure.identity.repository.RefreshTokenRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.identity.repository.UserRoleRepository;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.NoSuchFileException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = {
                BarterApplication.class,
                StorageFailureItemImagesIntegrationTest.StorageFailureConfig.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration",
                "barter.jwt.secret=integration-test-secret-key-at-least-32-bytes!!",
                "barter.storage.local.base-path=${java.io.tmpdir}/barter-test-uploads",
                "barter.seed.demo-content=true"
        }
)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class StorageFailureItemImagesIntegrationTest {

    private static final String CATEGORY_TOYS_UUID = "c0a80101-0001-4000-8000-000000000001";

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

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private EmailVerificationCodeRepository emailVerificationCodeRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private ItemTagRepository itemTagRepository;
    @Autowired private ItemImageRepository itemImageRepository;

    @BeforeEach
    void cleanMutableTables() {
        SecurityContextHolder.clearContext();
        itemImageRepository.deleteAllInBatch();
        itemTagRepository.deleteAllInBatch();
        itemRepository.deleteAllInBatch();
        emailVerificationCodeRepository.deleteAllInBatch();
        refreshTokenRepository.deleteAllInBatch();
        userRoleRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void uploadReturnsCleanApiErrorWhenStorageFails() throws Exception {
        String token = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");
        String itemUuid = createItem(token, "Storage Failure Item");
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", minimalJpeg());

        mockMvc.perform(multipartPost("/catalog/items/" + itemUuid + "/images")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Image storage is currently unavailable. Please try again later."))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("super-secret-account-key"))));
    }

    private static byte[] minimalJpeg() {
        byte[] b = new byte[20];
        b[0] = (byte) 0xFF;
        b[1] = (byte) 0xD8;
        b[2] = (byte) 0xFF;
        b[3] = (byte) 0xE0;
        return b;
    }

    private String registerActivateAndLogin(String username, String email, String password) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contextPath("/api/v1")
                        .servletPath("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, email, password)))
                .andExpect(status().isCreated());

        var user = userRepository.findByEmail(email).orElseThrow();
        user.setStatus(com.barterplatform.domain.identity.enums.UserStatus.ACTIVE);
        user.setEmailVerified(true);
        userRepository.save(user);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contextPath("/api/v1")
                        .servletPath("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return loginJson.get("accessToken").asText();
    }

    private String createItem(String token, String title) throws Exception {
        MvcResult result = mockMvc.perform(apiPost("/catalog/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "categoryUuid": "%s",
                                  "condition": "GOOD"
                                }
                                """.formatted(title, CATEGORY_TOYS_UUID)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractField(result, "uuid");
    }

    private String extractField(MvcResult result, String field) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get(field).asText();
    }

    private MockHttpServletRequestBuilder apiPost(String path) {
        return post("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path)
                .accept(MediaType.APPLICATION_JSON);
    }

    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder multipartPost(String path) {
        return (org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder)
                multipart("/api/v1" + path)
                        .contextPath("/api/v1")
                        .servletPath(path)
                        .accept(MediaType.APPLICATION_JSON);
    }

    @TestConfiguration
    static class StorageFailureConfig {

        @Bean
        @Primary
        FileStorageService failingFileStorageService() {
            return new FileStorageService() {
                @Override
                public void store(String storageKey, InputStream inputStream, long contentLength, String contentType)
                        throws IOException {
                    throw new IOException(
                            "Upload failed DefaultEndpointsProtocol=https;AccountName=barterdevstorage784;AccountKey=super-secret-account-key;EndpointSuffix=core.windows.net");
                }

                @Override
                public void delete(String storageKey) {
                }

                @Override
                public String resolveUrl(String storageKey) {
                    return URI.create("http://localhost/files/" + storageKey).toString();
                }

                @Override
                public StoredFileMetadata getMetadata(String storageKey) throws IOException {
                    throw new NoSuchFileException(storageKey);
                }

                @Override
                public StoredFile load(String storageKey) throws IOException {
                    throw new NoSuchFileException(storageKey);
                }
            };
        }
    }
}

