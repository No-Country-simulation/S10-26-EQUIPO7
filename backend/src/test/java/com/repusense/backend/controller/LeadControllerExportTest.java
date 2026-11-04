package com.repusense.backend.controller;

import com.repusense.backend.model.Business;
import com.repusense.backend.model.Lead;
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
        when(leadRepository.findAllWithBusiness())
                .thenReturn(List.of(lead(1L, "Comercio general")));

        var response = leadController.exportLeadsToCsv(null);
        String csv = csv(response.getBody());

        assertEquals(200, response.getStatusCode().value());
        assertTrue(csv.startsWith("\uFEFF"));
        assertTrue(csv.contains("ID Lead,Nombre,Ciudad,Categoria"));
        assertTrue(csv.contains("\"Comercio general\""));
        assertEquals(
                "attachment; filename=\"repusense-leads.csv\"",
                response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)
        );
        verify(leadRepository).findAllWithBusiness();
        verifyNoInteractions(searchRunRepository);
    }

    @Test
    void exportsOnlyLeadsForRequestedSearchAndEscapesCsvValues() {
        when(searchRunRepository.existsById(5L)).thenReturn(true);
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
        verify(leadRepository).findAllBySearchRunId(5L);
        verify(leadRepository, never()).findAllWithBusiness();
    }

    @Test
    void returnsNotFoundForUnknownSearch() {
        when(searchRunRepository.existsById(999L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> leadController.exportLeadsToCsv(999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verifyNoInteractions(leadRepository);
    }

    @Test
    void exportsHeaderOnlyWhenSearchHasNoAssociatedLeads() {
        when(searchRunRepository.existsById(2L)).thenReturn(true);
        when(leadRepository.findAllBySearchRunId(2L)).thenReturn(List.of());

        var response = leadController.exportLeadsToCsv(2L);
        String csv = csv(response.getBody());

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, csv.lines().count());
        assertTrue(csv.startsWith("\uFEFF"));
    }

    @Test
    void keepsTwoSearchExportsSeparate() {
        when(searchRunRepository.existsById(2L)).thenReturn(true);
        when(searchRunRepository.existsById(5L)).thenReturn(true);
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

    private Lead lead(Long id, String businessName) {
        Business business = Business.builder()
                .name(businessName)
                .city("La Plata")
                .category("Comercio")
                .build();

        return Lead.builder()
                .id(id)
                .business(business)
                .status("NEW")
                .build();
    }

    private String csv(byte[] content) {
        assertNotNull(content);
        return new String(content, StandardCharsets.UTF_8);
    }
}
