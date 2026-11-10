package com.repusense.backend.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repusense.backend.auth.service.JwtService;
import com.repusense.backend.model.AppUser;
import com.repusense.backend.model.UserRole;
import com.repusense.backend.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:repusense-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "gemini.api.key=test-gemini-key",
        "app.jwt.secret=unit-test-only-secret-key-with-at-least-32-bytes",
        "app.jwt.expiration-ms=3600000",
        "app.cors.allowed-origins=http://localhost:5173,http://127.0.0.1:5173"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "strong-pass-123";
    private static final String JWT_SECRET =
            "unit-test-only-secret-key-with-at-least-32-bytes";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void clearUsers() {
        appUserRepository.deleteAll();
    }

    @Test
    void registersUserWithBcryptAndReturnsOnlySafeUserFields() throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Test User",
                                "email", EMAIL,
                                "password", PASSWORD,
                                "role", "ADMIN"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.name").value("Test User"))
                .andExpect(jsonPath("$.user.email").value(EMAIL))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertFalse(response.contains(PASSWORD));
        AppUser savedUser = appUserRepository.findByEmailIgnoreCase(EMAIL)
                .orElseThrow();
        assertNotEquals(PASSWORD, savedUser.getPasswordHash());
        assertTrue(savedUser.getPasswordHash().startsWith("$2"));
        assertTrue(savedUser.isActive());
        assertEquals(UserRole.USER, savedUser.getRole());
        assertNotNull(savedUser.getCreatedAt());
    }

    @Test
    void rejectsDuplicateEmailWithoutCaseSensitivity() throws Exception {
        register();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Another User",
                                "email", "USER@example.com",
                                "password", PASSWORD
                        ))))
                .andExpect(status().isConflict());
    }

    @Test
    void validatesRequiredFieldsEmailAndPasswordLength() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Test User",
                                "email", "not-an-email",
                                "password", PASSWORD
                        ))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Test User",
                                "email", EMAIL,
                                "password", "short"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logsInWithValidCredentialsAndReturnsJwtAndBasicUser() throws Exception {
        register();

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", EMAIL,
                                "password", PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.id").isNumber())
                .andExpect(jsonPath("$.user.name").value("Test User"))
                .andExpect(jsonPath("$.user.email").value(EMAIL))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode responseJson = objectMapper.readTree(response);
        assertEquals(
                EMAIL,
                jwtService.parseClaims(responseJson.get("token").asText())
                        .getSubject()
        );
        assertFalse(response.contains(PASSWORD));
    }

    @Test
    void rejectsIncorrectLoginCredentials() throws Exception {
        register();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", EMAIL,
                                "password", "wrong-password"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectsExistingApiEndpointsAndAcceptsValidBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/leads?page=0&size=100"))
                .andExpect(status().isUnauthorized());

        String token = loginAndGetToken();
        mockMvc.perform(get("/api/v1/leads?page=0&size=100")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/searches")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsInvalidAndExpiredTokens() throws Exception {
        mockMvc.perform(get("/api/v1/leads")
                        .header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized());

        String expiredToken = io.jsonwebtoken.Jwts.builder()
                .subject(EMAIL)
                .issuedAt(new Date(System.currentTimeMillis() - 10_000))
                .expiration(new Date(System.currentTimeMillis() - 1_000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        JWT_SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)
                ))
                .compact();

        mockMvc.perform(get("/api/v1/leads")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void corsAllowsConfiguredFrontendAndRejectsOtherOrigins() throws Exception {
        mockMvc.perform(options("/api/v1/leads")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Access-Control-Allow-Origin",
                        "http://localhost:5173"
                ));

        mockMvc.perform(options("/api/v1/leads")
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    private void register() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Test User",
                                "email", EMAIL,
                                "password", PASSWORD
                        ))))
                .andExpect(status().isCreated());
    }

    private String loginAndGetToken() throws Exception {
        register();
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", EMAIL,
                                "password", PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
