
package com.repusense.backend.controller;

import com.repusense.backend.model.Business;
import com.repusense.backend.model.Lead;
import com.repusense.backend.model.Review;
import com.repusense.backend.repository.LeadRepository;
import com.repusense.backend.repository.ReviewRepository;
import com.repusense.backend.repository.SearchRunRepository;
import com.repusense.backend.service.GeminiService;
import com.repusense.backend.service.GeminiService.AnalysisResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/leads")
public class LeadController {

    private static final Logger log =
            LoggerFactory.getLogger(LeadController.class);

    private static final int MAX_PAGE_SIZE = 100;

    private final LeadRepository leadRepository;
    private final ReviewRepository reviewRepository;
    private final SearchRunRepository searchRunRepository;
    private final GeminiService geminiService;

    public LeadController(
            LeadRepository leadRepository,
            ReviewRepository reviewRepository,
            SearchRunRepository searchRunRepository,
            GeminiService geminiService) {

        this.leadRepository = leadRepository;
        this.reviewRepository = reviewRepository;
        this.searchRunRepository = searchRunRepository;
        this.geminiService = geminiService;
    }

    @PostMapping
    public ResponseEntity<Lead> createLead(
            @RequestBody(required = false) Lead lead) {

        if (lead == null) {
            throw new IllegalArgumentException(
                    "Los datos del lead son obligatorios."
            );
        }

        return ResponseEntity.ok(leadRepository.save(lead));
    }

    @GetMapping
    public ResponseEntity<Page<Lead>> getAllLeads(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "El número de página no puede ser negativo."
            );
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "El tamaño de página debe estar entre 1 y "
                            + MAX_PAGE_SIZE + "."
            );
        }

        Pageable pageable = PageRequest.of(page, size);

        Specification<Lead> spec =
                (root, query, cb) -> cb.conjunction();

        if (city != null && !city.isBlank()) {
            String cleanCity = city.trim().toLowerCase();

            spec = spec.and((root, query, cb) ->
                    cb.like(
                            cb.lower(root.get("business").get("city")),
                            "%" + cleanCity + "%"
                    )
            );
        }

        if (status != null && !status.isBlank()) {
            String cleanStatus = status.trim();

            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("status"), cleanStatus)
            );
        }

        return ResponseEntity.ok(
                leadRepository.findAll(spec, pageable)
        );
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<Map<String, Object>> analyzeLead(
            @PathVariable Long id) {

        validateId(id);

        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró el lead con ID: " + id
                ));

        Business business = lead.getBusiness();

        if (business == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "El lead no tiene un negocio asociado."
            );
        }

        log.info(
                "[Lead] Preparando análisis del lead {}: {}",
                id,
                business.getName()
        );

        List<Review> reviews =
                reviewRepository.findByBusinessId(business.getId());

        String reviewsText = reviews.stream()
                .map(Review::getText)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("\n"));

        StringBuilder information = new StringBuilder();

        information.append("Nombre: ")
                .append(valueOrUnknown(business.getName()));

        information.append("\nCiudad: ")
                .append(valueOrUnknown(business.getCity()));

        information.append("\nCategoría: ")
                .append(valueOrUnknown(business.getCategory()));

        information.append("\nDirección: ")
                .append(valueOrUnknown(business.getAddress()));

        information.append("\nCalificación registrada: ")
                .append(business.getRating() == null
                        ? "Sin datos"
                        : business.getRating());

        information.append("\nCantidad de reseñas registrada: ")
                .append(business.getReviewCount() == null
                        ? "Sin datos"
                        : business.getReviewCount());

        information.append("\nSitio web: ")
                .append(valueOrUnknown(business.getWebsite()));

        if (!reviewsText.isBlank()) {
            information.append("\n\nReseñas guardadas:\n")
                    .append(reviewsText);
        } else {
            information.append(
                    "\n\nNo hay textos de reseñas guardados. "
                            + "Realiza una evaluación preliminar únicamente "
                            + "con los datos del perfil comercial y aclara "
                            + "esta limitación."
            );
        }

        log.info(
                "[Lead] Información preparada. Hay textos de reseñas: {}",
                !reviewsText.isBlank()
        );

        AnalysisResult result =
                geminiService.analyzeBusinessReviews(
                        business.getName(),
                        information.toString()
                );

        if (result == null
                || result.analysis() == null
                || result.analysis().isBlank()) {

            throw new IllegalStateException(
                    "Gemini no devolvió un análisis válido."
            );
        }

        lead.setAiAnalysis(result.analysis());
        lead.setRiskScore(result.riskScore());
        lead.setRiskReason(result.riskReason());
        lead.setAiMainProblem(result.mainProblem());

        if (!"CONTACTED".equalsIgnoreCase(lead.getStatus())) {
            lead.setStatus("ANALYZED");
        }

        lead = leadRepository.save(lead);

        log.info(
                "[Lead] Lead {} guardado. Estado: {}. Riesgo: {}",
                id,
                lead.getStatus(),
                lead.getRiskScore()
        );

        Map<String, Object> response = new HashMap<>();

        response.put("leadId", lead.getId());
        response.put("analysis", lead.getAiAnalysis());
        response.put("status", lead.getStatus());
        response.put("riskScore", lead.getRiskScore());
        response.put("riskReason", lead.getRiskReason());
        response.put("mainProblem", lead.getAiMainProblem());
        response.put("saved", true);
        response.put("hasReviews", !reviewsText.isBlank());

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateLeadStatus(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> updates) {

        validateId(id);

        if (updates == null
                || !updates.containsKey("status")
                || updates.get("status") == null
                || updates.get("status").isBlank()) {

            throw new IllegalArgumentException(
                    "Debés proporcionar un estado válido para el lead."
            );
        }

        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró el lead con ID: " + id
                ));

        lead.setStatus(updates.get("status").trim());
        leadRepository.save(lead);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportLeadsToCsv(
            @RequestParam(required = false) Long searchId) {

        if (searchId == null) {
            return createCsvResponse(
                    leadRepository.findAllWithBusiness(),
                    "repusense-leads.csv",
                    "\r\n"
            );
        }

        if (searchId <= 0) {
            throw new IllegalArgumentException(
                    "El ID de búsqueda debe ser un número positivo."
            );
        }

        var searchRun = searchRunRepository.findById(searchId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró la búsqueda solicitada."
                ));

        List<Lead> searchLeads =
                leadRepository.findAllBySearchRunId(searchId);

        if (searchLeads.isEmpty()) {
            Integer savedCount = searchRun.getSavedCount();

            if (savedCount == null) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "No se pudo verificar la cantidad de comercios "
                                + "asociados a esta búsqueda."
                );
            }

            if (savedCount == 0) {
                return createCsvResponse(
                        searchLeads,
                        "repusense-busqueda-" + searchId + ".csv"
                );
            }

            List<Lead> matchingLeads =
                    leadRepository.findAllByBusinessCityAndCategory(
                            searchRun.getCity(),
                            searchRun.getCategory()
                    );

            if (!Integer.valueOf(matchingLeads.size())
                    .equals(savedCount)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "No se pudieron verificar los comercios asociados "
                                + "a esta búsqueda."
                );
            }

            searchLeads = matchingLeads;
        }

        return createCsvResponse(
                searchLeads,
                "repusense-busqueda-" + searchId + ".csv"
        );
    }

    private ResponseEntity<byte[]> createCsvResponse(
            List<Lead> leads,
            String filename) {
        return createCsvResponse(leads, filename, "\n");
    }

    private ResponseEntity<byte[]> createCsvResponse(
            List<Lead> leads,
            String filename,
            String recordSeparator) {
        StringBuilder csv = new StringBuilder();

        csv.append('\uFEFF');

        csv.append(
                "ID Lead,Nombre,Ciudad,Categoria,Direccion,Calificacion,"
                        + "Cantidad Resenas,Telefono,Web,Estado,Email Contacto,"
                        + "Riesgo,Motivo Riesgo,Problema Principal,Analisis IA"
        ).append(recordSeparator);

        for (Lead lead : leads) {

            Business business = lead.getBusiness();

            csv.append(lead.getId()).append(',')
                    .append(csvValue(
                            business == null ? "" : business.getName()
                    )).append(',')
                    .append(csvValue(
                            business == null ? "" : business.getCity()
                    )).append(',')
                    .append(csvValue(
                            business == null ? "" : business.getCategory()
                    )).append(',')
                    .append(csvValue(
                            business == null ? "" : business.getAddress()
                    )).append(',')
                    .append(csvValue(
                            business == null || business.getRating() == null
                                    ? "" : business.getRating().toString()
                    )).append(',')
                    .append(csvValue(
                            business == null || business.getReviewCount() == null
                                    ? "" : business.getReviewCount().toString()
                    )).append(',')
                    .append(csvValue(
                            business == null ? "" : business.getPhone()
                    )).append(',')
                    .append(csvValue(
                            business == null ? "" : business.getWebsite()
                    )).append(',')
                    .append(csvValue(lead.getStatus())).append(',')
                    .append(csvValue(lead.getContactEmail())).append(',')
                    .append(csvValue(
                            lead.getRiskScore() == null
                                    ? "" : lead.getRiskScore().toString()
                    )).append(',')
                    .append(csvValue(lead.getRiskReason())).append(',')
                    .append(csvValue(lead.getAiMainProblem())).append(',')
                    .append(csvValue(lead.getAiAnalysis()))
                    .append(recordSeparator);
        }

        byte[] content = csv.toString()
                .getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(
                MediaType.parseMediaType("text/csv; charset=UTF-8")
        );

        headers.set(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + "\""
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(content);
    }

    private void validateId(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "El ID del lead debe ser un número positivo."
            );
        }
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank()
                ? "Sin datos"
                : value;
    }

    private String csvValue(String value) {

        if (value == null) {
            return "\"\"";
        }

        String safeValue = value.replace("\"", "\"\"");

        return "\"" + safeValue + "\"";
    }
}
