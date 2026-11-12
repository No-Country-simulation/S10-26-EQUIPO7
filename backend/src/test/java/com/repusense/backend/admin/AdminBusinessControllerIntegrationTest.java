package com.repusense.backend.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.repusense.backend.auth.service.JwtService;
import com.repusense.backend.model.AppUser;
import com.repusense.backend.model.Business;
import com.repusense.backend.model.Lead;
import com.repusense.backend.model.Review;
import com.repusense.backend.model.SearchRun;
import com.repusense.backend.model.UserRole;
import com.repusense.backend.repository.AppUserRepository;
import com.repusense.backend.repository.BusinessRepository;
import com.repusense.backend.repository.LeadRepository;
import com.repusense.backend.repository.ReviewRepository;
import com.repusense.backend.repository.SearchRunRepository;
import com.repusense.backend.service.GoogleMapsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:repusense-admin-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "gemini.api.key=test-gemini-key",
        "app.jwt.secret=unit-test-only-secret-key-with-at-least-32-bytes",
        "app.jwt.expiration-ms=3600000",
        "app.cors.allowed-origins=http://localhost:5173",
        "app.admin.bootstrap-email=",
        "app.admin.bootstrap-password="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminBusinessControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private BusinessRepository businessRepository;
    @Autowired
    private LeadRepository leadRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private SearchRunRepository searchRunRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;
    @MockBean
    private GoogleMapsService googleMapsService;

    @BeforeEach
    void clearDatabase() {
        searchRunRepository.deleteAll();
        reviewRepository.deleteAll();
        leadRepository.deleteAll();
        businessRepository.deleteAll();
        appUserRepository.deleteAll();
    }

    @Test
    void adminCanListBusinessesWithLeadStatus() throws Exception {
        Business business = createBusiness("Central");
        createLead(business, "CONTACTED");
        createLead(business, "ANALYZED");
        String token = createUser(UserRole.ADMIN);

        mockMvc.perform(get("/api/admin/businesses")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(business.getId()))
                .andExpect(jsonPath("$[0].name").value("Central"))
                .andExpect(jsonPath("$[0].leadStatus").value("ANALYZED"));
    }

    @Test
    void adminCanDeleteBusinessAndKeepSearchHistoryWithoutOrphanRelations()
            throws Exception {
        Business business = createBusiness("Central");
        createLead(business, "NEW");
        createReview(business);
        SearchRun searchRun = createSearchRun(business);
        String token = createUser(UserRole.ADMIN);

        mockMvc.perform(delete("/api/admin/businesses/{id}", business.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedCount").value(1))
                .andExpect(jsonPath("$.deletedIds[0]").value(business.getId()));

        assertFalse(businessRepository.existsById(business.getId()));
        assertTrue(leadRepository.findAll().isEmpty());
        assertTrue(reviewRepository.findAll().isEmpty());
        SearchRun preservedSearch = searchRunRepository.findWithBusinessesById(searchRun.getId())
                .orElseThrow();
        assertTrue(preservedSearch.getBusinesses().isEmpty());
        assertEquals(1, preservedSearch.getSavedCount());
    }

    @Test
    void adminCanDeleteSeveralBusinessesInOneOperation() throws Exception {
        Business first = createBusiness("First");
        Business second = createBusiness("Second");
        Business retained = createBusiness("Retained");
        createLead(first, "NEW");
        createLead(second, "ANALYZED");
        String token = createUser(UserRole.ADMIN);

        mockMvc.perform(delete("/api/admin/businesses/bulk")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "ids", List.of(first.getId(), second.getId())
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedCount").value(2));

        assertEquals(1, businessRepository.count());
        assertTrue(businessRepository.existsById(retained.getId()));
        assertTrue(leadRepository.findAll().isEmpty());
    }

    @Test
    void userCannotDeleteAndUnauthenticatedRequestIsUnauthorized()
            throws Exception {
        Business business = createBusiness("Protected");
        String userToken = createUser(UserRole.USER);

        mockMvc.perform(get("/api/admin/businesses")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/admin/businesses/{id}", business.getId())
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
        assertTrue(businessRepository.existsById(business.getId()));

        mockMvc.perform(get("/api/admin/businesses"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/admin/businesses/{id}", business.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanDeleteSearchHistoryWithoutDeletingCommercialData()
            throws Exception {
        Business business = createBusiness("History protected");
        Lead lead = createLead(business, "CONTACTED");
        lead.setAiAnalysis("Análisis existente");
        lead.setRiskScore(4);
        leadRepository.save(lead);
        createReview(business);
        SearchRun searchRun = createSearchRun(business);
        String token = createUser(UserRole.ADMIN);

        mockMvc.perform(delete("/api/searches/{id}", searchRun.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        assertFalse(searchRunRepository.existsById(searchRun.getId()));
        assertEquals(1, businessRepository.count());
        assertEquals(1, leadRepository.count());
        assertEquals(1, reviewRepository.count());
        Lead preservedLead = leadRepository.findAll().get(0);
        assertEquals("CONTACTED", preservedLead.getStatus());
        assertEquals("Análisis existente", preservedLead.getAiAnalysis());
        assertEquals(4, preservedLead.getRiskScore());
    }

    @Test
    void adminCanClearSearchHistoryWithoutDeletingCommercialData()
            throws Exception {
        Business firstBusiness = createBusiness("First retained");
        Business secondBusiness = createBusiness("Second retained");
        Lead lead = createLead(firstBusiness, "ANALYZED");
        lead.setAiAnalysis("Análisis que debe conservarse");
        leadRepository.save(lead);
        createReview(firstBusiness);
        createSearchRun(firstBusiness);
        createSearchRun(secondBusiness);
        String token = createUser(UserRole.ADMIN);

        mockMvc.perform(delete("/api/searches")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        assertEquals(0, searchRunRepository.count());
        assertEquals(2, businessRepository.count());
        assertEquals(1, leadRepository.count());
        assertEquals(1, reviewRepository.count());
        Lead preservedLead = leadRepository.findAll().get(0);
        assertEquals("ANALYZED", preservedLead.getStatus());
        assertEquals("Análisis que debe conservarse", preservedLead.getAiAnalysis());
    }

    @Test
    void userCannotDeleteSearchHistoryThroughEitherEndpoint()
            throws Exception {
        Business business = createBusiness("User protected");
        SearchRun searchRun = createSearchRun(business);
        String token = createUser(UserRole.USER);

        mockMvc.perform(delete("/api/searches/{id}", searchRun.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/searches")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());

        assertTrue(searchRunRepository.existsById(searchRun.getId()));
        assertTrue(businessRepository.existsById(business.getId()));
    }

    @Test
    void deletingMissingSearchReturnsNotFound() throws Exception {
        String token = createUser(UserRole.ADMIN);

        mockMvc.perform(delete("/api/searches/{id}", 987654L)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void authenticatedUserCanStillCreateNewSearches() throws Exception {
        when(googleMapsService.fetchBusinessesByCityAndCategory(
                "La Plata",
                "Farmacia"
        )).thenReturn(List.of(Business.builder()
                .source("TEST")
                .sourceBusinessId("new-search-business")
                .name("Farmacia nueva")
                .city("La Plata")
                .category("Farmacia")
                .build()));
        String token = createUser(UserRole.USER);

        mockMvc.perform(post("/api/searches")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "city", "La Plata",
                                "category", "Farmacia"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.savedCount").value(1));

        assertEquals(1, searchRunRepository.count());
        assertEquals(1, businessRepository.count());
        assertEquals(1, leadRepository.count());
    }

    @Test
    void missingBusinessReturnsNotFound() throws Exception {
        String token = createUser(UserRole.ADMIN);

        mockMvc.perform(delete("/api/admin/businesses/{id}", 987654L)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidBulkSelectionDoesNotPartiallyDelete() throws Exception {
        Business business = createBusiness("Must remain");
        createLead(business, "NEW");
        String token = createUser(UserRole.ADMIN);

        mockMvc.perform(delete("/api/admin/businesses/bulk")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "ids", List.of(business.getId(), 987654L)
                        ))))
                .andExpect(status().isNotFound());

        assertTrue(businessRepository.existsById(business.getId()));
        assertEquals(1, leadRepository.count());
    }

    @Test
    void rejectsMalformedBulkSelectionBeforeDeletion() throws Exception {
        Business business = createBusiness("Must remain");
        String token = createUser(UserRole.ADMIN);

        mockMvc.perform(delete("/api/admin/businesses/bulk")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("ids", List.of(business.getId(), -1L)))))
                .andExpect(status().isBadRequest());

        assertTrue(businessRepository.existsById(business.getId()));
    }

    @Test
    void deduplicatesRepeatedIdsAndDeletesAllRelatedLeads() throws Exception {
        Business business = createBusiness("Repeated ID");
        createLead(business, "NEW");
        createLead(business, "CONTACTED");
        String token = createUser(UserRole.ADMIN);

        mockMvc.perform(delete("/api/admin/businesses/bulk")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "ids", List.of(
                                        business.getId(),
                                        business.getId()
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedCount").value(1))
                .andExpect(jsonPath("$.deletedIds.length()").value(1));

        assertFalse(businessRepository.existsById(business.getId()));
        assertTrue(leadRepository.findAll().isEmpty());
    }

    @Test
    void existingBusinessDeleteRouteIsAlsoAdminOnly() throws Exception {
        Business business = createBusiness("Protected legacy route");
        String token = createUser(UserRole.USER);

        mockMvc.perform(delete("/api/v1/businesses/{id}", business.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
        assertTrue(businessRepository.existsById(business.getId()));
    }

    private Business createBusiness(String suffix) {
        return businessRepository.save(Business.builder()
                .source("TEST")
                .sourceBusinessId("business-" + suffix)
                .name(suffix)
                .city("La Plata")
                .category("Farmacia")
                .address("Calle 1")
                .rating(4.5)
                .reviewCount(10)
                .build());
    }

    private Lead createLead(Business business, String leadStatus) {
        return leadRepository.save(Lead.builder()
                .business(business)
                .status(leadStatus)
                .build());
    }

    private Review createReview(Business business) {
        return reviewRepository.save(Review.builder()
                .business(business)
                .text("Reseña de prueba")
                .rating(4.0)
                .build());
    }

    private SearchRun createSearchRun(Business business) {
        SearchRun searchRun = SearchRun.builder()
                .city(business.getCity())
                .category(business.getCategory())
                .status("COMPLETED")
                .foundCount(1)
                .savedCount(1)
                .businesses(new LinkedHashSet<>(List.of(business)))
                .build();
        return searchRunRepository.save(searchRun);
    }

    private String createUser(UserRole role) {
        AppUser user = new AppUser();
        user.setName(role.name());
        user.setEmail(role.name().toLowerCase() + "@example.com");
        user.setPasswordHash(passwordEncoder.encode("test-password-123"));
        user.setActive(true);
        user.setRole(role);
        return jwtService.generateToken(appUserRepository.save(user));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
