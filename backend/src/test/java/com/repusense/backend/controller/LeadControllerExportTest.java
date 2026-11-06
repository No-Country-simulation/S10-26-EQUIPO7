package com.repusense.backend.controller;

import com.repusense.backend.model.Business;
import com.repusense.backend.model.Lead;
import com.repusense.backend.model.SearchRun;
import com.repusense.backend.repository.LeadRepository;
import com.repusense.backend.repository.ReviewRepository;
import com.repusense.backend.repository.SearchRunRepository;
import com.repusense.backend.service.GeminiService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadControllerExportTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private SearchRunRepository searchRunRepository;

    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private LeadController leadController;

    @Test
    void exportsAllLeadsWhenSearchIdIsNotProvided() {
        Lead firstLead = lead(1L, "Comercio, \"Centro\"\nSucursal");
        firstLead.setContactEmail(null);
        firstLead.setAiAnalysis("Informe completo\ncon segunda línea");
        when(leadRepository.findAllWithBusiness()).thenReturn(List.of(firstLead));

        var response = leadController.exportLeadsToCsv(null);
        String csv = csv(response.getBody());

        assertEquals(200, response.getStatusCode().value());
        assertTrue(csv.startsWith("\uFEFF"));
        assertTrue(csv.contains("ID Lead,Nombre,Ciudad,Categoria"));
        assertTrue(csv.contains("\"Comercio, \"\"Centro\"\"\nSucursal\""));
        assertTrue(csv.contains("\"Informe completo\ncon segunda línea\""));
        assertTrue(csv.contains(",\"\",\"\"")); // Empty values remain empty CSV cells.
        assertTrue(csv.contains("\r\n"));
        assertEquals(3, csv.substring(1).split("\r\n", -1).length);
        assertEquals(15, csv.lines().findFirst().orElseThrow().split(",", -1).length);
        assertEquals(
                "attachment; filename=\"repusense-leads.csv\"",
                response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)
        );
        verify(leadRepository).findAllWithBusiness();
        verifyNoInteractions(searchRunRepository);
    }

    @Test
    void generalCsvKeepsMultilineAnalysisInOneQuotedCell() {
        String analysis = "Primera línea, con coma\nSegunda línea con \"cita\"";
        when(leadRepository.findAllWithBusiness())
                .thenReturn(List.of(lead(1L, "Comercio", analysis)));

        String csv = csv(leadController.exportLeadsToCsv(null).getBody());
        String[] records = csv.substring(1).split("\r\n", -1);

        assertEquals(3, records.length);
        assertEquals(15, records[0].split(",", -1).length);
        assertTrue(records[1].startsWith("1,"));
        assertTrue(records[1].endsWith(
                ",\"Primera línea, con coma\nSegunda línea con \"\"cita\"\"\""
        ));
    }

    @Test
    void exportsOnlyLeadsForRequestedSearchAndEscapesCsvValues() {
        when(searchRunRepository.findById(5L))
                .thenReturn(Optional.of(searchRun(5L, 1, "Quilmes", "Panadería")));
        when(leadRepository.findAllBySearchRunId(5L))
                .thenReturn(List.of(lead(10L, "Local, \"Centro\"\nSucursal")));

        var response = leadController.exportLeadsToCsv(5L);
        String csv = csv(response.getBody());

        assertEquals(200, response.getStatusCode().value());
        assertTrue(csv.contains("\"Local, \"\"Centro\"\"\nSucursal\""));
        assertEquals(
                "attachment; filename=\"repusense-busqueda-5.csv\"",
                response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)
        );
        assertTrue(csv.contains("\n"));
        assertFalse(csv.contains("\r\n"));
        verify(leadRepository).findAllBySearchRunId(5L);
        verify(leadRepository, never())
                .findAllByBusinessCityAndCategory("Quilmes", "Panadería");
        verify(leadRepository, never()).findAllWithBusiness();
    }

    @Test
    void returnsNotFoundForUnknownSearch() {
        when(searchRunRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> leadController.exportLeadsToCsv(999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verifyNoInteractions(leadRepository);
    }

    @Test
    void exportsHeaderOnlyWhenSearchHasNoAssociatedLeads() {
        when(searchRunRepository.findById(2L))
                .thenReturn(Optional.of(searchRun(2L, 0, "La Plata", "farmacia")));
        when(leadRepository.findAllBySearchRunId(2L)).thenReturn(List.of());

        var response = leadController.exportLeadsToCsv(2L);
        String csv = csv(response.getBody());

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, csv.lines().count());
        assertTrue(csv.startsWith("\uFEFF"));
        verify(leadRepository, never())
                .findAllByBusinessCityAndCategory("La Plata", "farmacia");
    }

    @Test
    void keepsTwoSearchExportsSeparate() {
        when(searchRunRepository.findById(2L))
                .thenReturn(Optional.of(searchRun(2L, 1, "La Plata", "farmacia")));
        when(searchRunRepository.findById(5L))
                .thenReturn(Optional.of(searchRun(5L, 1, "Quilmes", "Panadería")));
        when(leadRepository.findAllBySearchRunId(2L))
                .thenReturn(List.of(lead(20L, "Comercio búsqueda dos")));
        when(leadRepository.findAllBySearchRunId(5L))
                .thenReturn(List.of(lead(50L, "Comercio búsqueda cinco")));

        String searchTwoCsv = csv(
                leadController.exportLeadsToCsv(2L).getBody()
        );
        String searchFiveCsv = csv(
                leadController.exportLeadsToCsv(5L).getBody()
        );

        assertTrue(searchTwoCsv.contains("\"Comercio búsqueda dos\""));
        assertFalse(searchTwoCsv.contains("\"Comercio búsqueda cinco\""));
        assertTrue(searchFiveCsv.contains("\"Comercio búsqueda cinco\""));
        assertFalse(searchFiveCsv.contains("\"Comercio búsqueda dos\""));
        verify(leadRepository).findAllBySearchRunId(2L);
        verify(leadRepository).findAllBySearchRunId(5L);
    }

    @Test
    void recoversLegacySearchWhenMatchingLeadsAgreeWithSavedCount() {
        when(searchRunRepository.findById(5L))
                .thenReturn(Optional.of(searchRun(5L, 8, "Quilmes", "Panadería")));
        when(leadRepository.findAllBySearchRunId(5L)).thenReturn(List.of());
        when(leadRepository.findAllByBusinessCityAndCategory(
                "Quilmes", "Panadería"
        )).thenReturn(List.of(
                lead(1L, "Comercio uno"),
                lead(2L, "Comercio dos"),
                lead(3L, "Comercio tres"),
                lead(4L, "Comercio cuatro"),
                lead(5L, "Comercio cinco"),
                lead(6L, "Comercio seis"),
                lead(7L, "Comercio siete"),
                lead(8L, "Comercio ocho")
        ));

        var response = leadController.exportLeadsToCsv(5L);
        String csv = csv(response.getBody());

        assertEquals(200, response.getStatusCode().value());
        assertEquals(9, csv.lines().count());
        assertTrue(csv.contains("\"Comercio ocho\""));
        verify(leadRepository).findAllByBusinessCityAndCategory(
                "Quilmes", "Panadería"
        );
    }

    @Test
    void refusesLegacyFallbackWhenLeadCountDoesNotMatchSearch() {
        when(searchRunRepository.findById(5L))
                .thenReturn(Optional.of(searchRun(5L, 8, "Quilmes", "Panadería")));
        when(leadRepository.findAllBySearchRunId(5L)).thenReturn(List.of());
        when(leadRepository.findAllByBusinessCityAndCategory(
                "Quilmes", "Panadería"
        )).thenReturn(List.of(lead(1L, "Comercio no verificable")));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> leadController.exportLeadsToCsv(5L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(leadRepository, never()).findAllWithBusiness();
    }

    private Lead lead(Long id, String businessName) {
        return lead(id, businessName, null);
    }

    private Lead lead(Long id, String businessName, String analysis) {
        Business business = Business.builder()
                .name(businessName)
                .city("La Plata")
                .category("Comercio")
                .build();

        return Lead.builder()
                .id(id)
                .business(business)
                .status("NEW")
                .aiAnalysis(analysis)
                .build();
    }

    private SearchRun searchRun(
            Long id,
            int savedCount,
            String city,
            String category) {
        return SearchRun.builder()
                .id(id)
                .savedCount(savedCount)
                .city(city)
                .category(category)
                .build();
    }

    private String csv(byte[] content) {
        assertNotNull(content);
        return new String(content, StandardCharsets.UTF_8);
    }
}
