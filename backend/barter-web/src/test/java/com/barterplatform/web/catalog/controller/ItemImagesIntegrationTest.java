package com.barterplatform.web.catalog.controller;

import com.barterplatform.BarterApplication;
import com.barterplatform.infrastructure.catalog.repository.ItemImageRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemRepository;
import com.barterplatform.infrastructure.catalog.repository.ItemTagRepository;
import com.barterplatform.infrastructure.identity.repository.EmailVerificationCodeRepository;
import com.barterplatform.infrastructure.identity.repository.RefreshTokenRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.identity.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.net.URI;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = BarterApplication.class,
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
class ItemImagesIntegrationTest {

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

    // ── Minimal valid JPEG bytes ─────────────────────────────────

    private static byte[] minimalJpeg() {
        byte[] b = new byte[20];
        b[0] = (byte) 0xFF;
        b[1] = (byte) 0xD8;
        b[2] = (byte) 0xFF;
        b[3] = (byte) 0xE0;
        return b;
    }

    private static byte[] minimalPng() {
        return new byte[]{
            (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 'I', 'H', 'D', 'R'
        };
    }

    // ══════════════════════════════════════════════════════════════
    //  Upload first image becomes primary
    // ══════════════════════════════════════════════════════════════

    @Test
    void uploadFirstImageIsPrimary() throws Exception {
        String token = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");
        String itemUuid = createItem(token, "First Image Item");

        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", minimalJpeg());

        mockMvc.perform(multipartPost("/catalog/items/" + itemUuid + "/images")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isPrimary").value(true))
                .andExpect(jsonPath("$.sortOrder").value(0))
                .andExpect(jsonPath("$.contentType").value("image/jpeg"));
    }

    @Test
    void uploadedImageCanBeFetchedViaBackendFilesEndpoint() throws Exception {
        String token = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");
        String itemUuid = createItem(token, "Fetch Image Item");
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", minimalJpeg());

        MvcResult uploadResult = mockMvc.perform(multipartPost("/catalog/items/" + itemUuid + "/images")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").isNotEmpty())
                .andReturn();

        String imageUrl = extractField(uploadResult, "url");
        String imagePath = toRelativePath(imageUrl);
        String servletPath = imagePath.substring("/api/v1".length());

        mockMvc.perform(get(imagePath)
                        .contextPath("/api/v1")
                        .servletPath(servletPath))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentType("image/jpeg"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .bytes(minimalJpeg()));
    }

    // ══════════════════════════════════════════════════════════════
    //  Upload second image does not replace primary
    // ══════════════════════════════════════════════════════════════

    @Test
    void uploadSecondImageDoesNotReplacePrimary() throws Exception {
        String token = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");
        String itemUuid = createItem(token, "Two Images Item");

        MockMultipartFile file1 = new MockMultipartFile("file", "first.jpg", "image/jpeg", minimalJpeg());
        MockMultipartFile file2 = new MockMultipartFile("file", "second.jpg", "image/jpeg", minimalJpeg());

        // First upload – is primary
        mockMvc.perform(multipartPost("/catalog/items/" + itemUuid + "/images")
                        .file(file1)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isPrimary").value(true));

        // Second upload – not primary
        mockMvc.perform(multipartPost("/catalog/items/" + itemUuid + "/images")
                        .file(file2)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isPrimary").value(false));
    }

    // ══════════════════════════════════════════════════════════════
    //  List images returns sorted order
    // ══════════════════════════════════════════════════════════════

    @Test
    void listImagesReturnsSortedOrder() throws Exception {
        String token = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");
        String itemUuid = createItem(token, "Sorted Images Item");

        for (int i = 0; i < 3; i++) {
            MockMultipartFile f = new MockMultipartFile("file", "img" + i + ".jpg", "image/jpeg", minimalJpeg());
            mockMvc.perform(multipartPost("/catalog/items/" + itemUuid + "/images")
                            .file(f)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(apiGet("/catalog/items/" + itemUuid + "/images")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].sortOrder").value(0))
                .andExpect(jsonPath("$[1].sortOrder").value(1))
                .andExpect(jsonPath("$[2].sortOrder").value(2));
    }

    // ══════════════════════════════════════════════════════════════
    //  Delete non-primary image
    // ══════════════════════════════════════════════════════════════

    @Test
    void deleteNonPrimaryImage() throws Exception {
        String token = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");
        String itemUuid = createItem(token, "Delete Non-primary Item");

        MockMultipartFile f1 = new MockMultipartFile("file", "first.jpg", "image/jpeg", minimalJpeg());
        MockMultipartFile f2 = new MockMultipartFile("file", "second.jpg", "image/jpeg", minimalJpeg());

        String firstImageUuid = uploadAndGetUuid(token, itemUuid, f1);
        String secondImageUuid = uploadAndGetUuid(token, itemUuid, f2);

        // Delete the NON-primary (second) image
        mockMvc.perform(apiDelete("/catalog/items/" + itemUuid + "/images/" + secondImageUuid)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // List should have only 1 image left, and it's still primary
        mockMvc.perform(apiGet("/catalog/items/" + itemUuid + "/images")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uuid").value(firstImageUuid))
                .andExpect(jsonPath("$[0].isPrimary").value(true));
    }

    // ══════════════════════════════════════════════════════════════
    //  Delete primary image promotes next image
    // ══════════════════════════════════════════════════════════════

    @Test
    void deletePrimaryImagePromotesNext() throws Exception {
        String token = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");
        String itemUuid = createItem(token, "Promote Primary Item");

        MockMultipartFile f1 = new MockMultipartFile("file", "first.jpg", "image/jpeg", minimalJpeg());
        MockMultipartFile f2 = new MockMultipartFile("file", "second.jpg", "image/jpeg", minimalJpeg());

        String firstImageUuid = uploadAndGetUuid(token, itemUuid, f1);
        String secondImageUuid = uploadAndGetUuid(token, itemUuid, f2);

        // Delete the primary (first) image
        mockMvc.perform(apiDelete("/catalog/items/" + itemUuid + "/images/" + firstImageUuid)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Second image should be promoted to primary
        mockMvc.perform(apiGet("/catalog/items/" + itemUuid + "/images")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].uuid").value(secondImageUuid))
                .andExpect(jsonPath("$[0].isPrimary").value(true));
    }

    // ══════════════════════════════════════════════════════════════
    //  Set primary image
    // ══════════════════════════════════════════════════════════════

    @Test
    void setPrimaryImage() throws Exception {
        String token = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");
        String itemUuid = createItem(token, "Set Primary Item");

        MockMultipartFile f1 = new MockMultipartFile("file", "first.jpg", "image/jpeg", minimalJpeg());
        MockMultipartFile f2 = new MockMultipartFile("file", "second.jpg", "image/jpeg", minimalJpeg());

        String firstImageUuid = uploadAndGetUuid(token, itemUuid, f1);
        String secondImageUuid = uploadAndGetUuid(token, itemUuid, f2);

        // Promote second image to primary
        mockMvc.perform(apiPut("/catalog/items/" + itemUuid + "/images/" + secondImageUuid + "/primary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPrimary").value(true))
                .andExpect(jsonPath("$.uuid").value(secondImageUuid));

        // Verify first image is no longer primary
        mockMvc.perform(apiGet("/catalog/items/" + itemUuid + "/images")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uuid").value(firstImageUuid))
                .andExpect(jsonPath("$[0].isPrimary").value(false))
                .andExpect(jsonPath("$[1].uuid").value(secondImageUuid))
                .andExpect(jsonPath("$[1].isPrimary").value(true));
    }

    // ══════════════════════════════════════════════════════════════
    //  Non-owner cannot upload / delete / set primary
    // ══════════════════════════════════════════════════════════════

    @Test
    void nonOwnerCannotUploadImage() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob42", "bob@example.com", "P@ssword456");
        String itemUuid = createItem(aliceToken, "Alice Item");

        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", minimalJpeg());

        mockMvc.perform(multipartPost("/catalog/items/" + itemUuid + "/images")
                        .file(file)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonOwnerCannotDeleteImage() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob42", "bob@example.com", "P@ssword456");
        String itemUuid = createItem(aliceToken, "Alice Item");

        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", minimalJpeg());
        String imageUuid = uploadAndGetUuid(aliceToken, itemUuid, file);

        mockMvc.perform(apiDelete("/catalog/items/" + itemUuid + "/images/" + imageUuid)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonOwnerCannotSetPrimary() throws Exception {
        String aliceToken = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");
        String bobToken = registerActivateAndLogin("bob42", "bob@example.com", "P@ssword456");
        String itemUuid = createItem(aliceToken, "Alice Item");

        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", minimalJpeg());
        String imageUuid = uploadAndGetUuid(aliceToken, itemUuid, file);

        mockMvc.perform(apiPut("/catalog/items/" + itemUuid + "/images/" + imageUuid + "/primary")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════════════════════════
    //  Cannot upload more than 6 images
    // ══════════════════════════════════════════════════════════════

    @Test
    void cannotUploadMoreThan6Images() throws Exception {
        String token = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");
        String itemUuid = createItem(token, "Six Images Item");

        for (int i = 0; i < 6; i++) {
            MockMultipartFile f = new MockMultipartFile("file", "img" + i + ".jpg", "image/jpeg", minimalJpeg());
            mockMvc.perform(multipartPost("/catalog/items/" + itemUuid + "/images")
                            .file(f)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated());
        }

        // 7th image should fail
        MockMultipartFile extra = new MockMultipartFile("file", "extra.jpg", "image/jpeg", minimalJpeg());
        mockMvc.perform(multipartPost("/catalog/items/" + itemUuid + "/images")
                        .file(extra)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    // ══════════════════════════════════════════════════════════════
    //  Invalid MIME type rejected
    // ══════════════════════════════════════════════════════════════

    @Test
    void invalidMimeTypeRejected() throws Exception {
        String token = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");
        String itemUuid = createItem(token, "Invalid Mime Item");

        // GIF89a header
        byte[] gifBytes = {0x47, 0x49, 0x46, 0x38, 0x39, 0x61};
        MockMultipartFile gifFile = new MockMultipartFile("file", "anim.gif", "image/gif", gifBytes);

        mockMvc.perform(multipartPost("/catalog/items/" + itemUuid + "/images")
                        .file(gifFile)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    // ══════════════════════════════════════════════════════════════
    //  Fake content-type rejected by magic byte validation
    // ══════════════════════════════════════════════════════════════

    @Test
    void fakeContentTypeRejectedByMagicByteValidation() throws Exception {
        String token = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");
        String itemUuid = createItem(token, "Fake Mime Item");

        // Declare as jpeg but actually text
        byte[] fakeBytes = "This is totally not an image".getBytes();
        MockMultipartFile fakeFile = new MockMultipartFile("file", "fake.jpg", "image/jpeg", fakeBytes);

        mockMvc.perform(multipartPost("/catalog/items/" + itemUuid + "/images")
                        .file(fakeFile)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    // ══════════════════════════════════════════════════════════════
    //  Item summary includes primaryImageUrl
    // ══════════════════════════════════════════════════════════════

    @Test
    void itemSummaryIncludesPrimaryImageUrl() throws Exception {
        String token = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");

        // Create an ACTIVE item so it shows in public search
        MvcResult result = mockMvc.perform(apiPost("/catalog/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Image URL Summary Test",
                                  "categoryUuid": "%s",
                                  "condition": "GOOD",
                                  "status": "ACTIVE"
                                }
                                """.formatted(CATEGORY_TOYS_UUID)))
                .andExpect(status().isCreated())
                .andReturn();
        String itemUuid = extractField(result, "uuid");

        // No image yet: primaryImageUrl should be null
        mockMvc.perform(apiGet("/catalog/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].primaryImageUrl").doesNotExist());

        // Upload an image
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", minimalJpeg());
        mockMvc.perform(multipartPost("/catalog/items/" + itemUuid + "/images")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        // Now primaryImageUrl should be present
        mockMvc.perform(apiGet("/catalog/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].primaryImageUrl").isNotEmpty());
    }

    // ══════════════════════════════════════════════════════════════
    //  Item detail includes primaryImageUrl and images
    // ══════════════════════════════════════════════════════════════

    @Test
    void itemDetailIncludesPrimaryImageUrlAndImages() throws Exception {
        String token = registerActivateAndLogin("alice", "alice@example.com", "P@ssword123");

        MvcResult result = mockMvc.perform(apiPost("/catalog/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Detail Images Test",
                                  "categoryUuid": "%s",
                                  "condition": "NEW",
                                  "status": "ACTIVE"
                                }
                                """.formatted(CATEGORY_TOYS_UUID)))
                .andExpect(status().isCreated())
                .andReturn();
        String itemUuid = extractField(result, "uuid");

        // No image: detail has empty images array and null primaryImageUrl
        mockMvc.perform(apiGet("/catalog/items/" + itemUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images", hasSize(0)))
                .andExpect(jsonPath("$.primaryImageUrl").doesNotExist());

        // Upload 2 images
        MockMultipartFile f1 = new MockMultipartFile("file", "first.jpg", "image/jpeg", minimalJpeg());
        MockMultipartFile f2 = new MockMultipartFile("file", "second.jpg", "image/jpeg", minimalJpeg());
        uploadAndGetUuid(token, itemUuid, f1);
        uploadAndGetUuid(token, itemUuid, f2);

        // Now detail has 2 images, primaryImageUrl is non-null
        mockMvc.perform(apiGet("/catalog/items/" + itemUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images", hasSize(2)))
                .andExpect(jsonPath("$.primaryImageUrl").isNotEmpty())
                .andExpect(jsonPath("$.images[0].isPrimary").value(true))
                .andExpect(jsonPath("$.images[1].isPrimary").value(false));
    }

    // ══════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════

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

    private String uploadAndGetUuid(String token, String itemUuid, MockMultipartFile file) throws Exception {
        MvcResult result = mockMvc.perform(multipartPost("/catalog/items/" + itemUuid + "/images")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();
        return extractField(result, "uuid");
    }

    private String extractField(MvcResult result, String field) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get(field).asText();
    }

    private String toRelativePath(String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return URI.create(url).getPath();
        }
        return url;
    }

    private MockHttpServletRequestBuilder apiGet(String path) {
        return get("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path)
                .accept(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder apiPost(String path) {
        return post("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path)
                .accept(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder apiDelete(String path) {
        return delete("/api/v1" + path)
                .contextPath("/api/v1")
                .servletPath(path)
                .accept(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder apiPut(String path) {
        return put("/api/v1" + path)
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
}

