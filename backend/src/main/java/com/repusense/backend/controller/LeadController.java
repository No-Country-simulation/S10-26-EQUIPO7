
package com.repusense.backend.controller;

import com.repusense.backend.model.Business;
import com.repusense.backend.model.Lead;
import com.repusense.backend.model.Review;
import com.repusense.backend.repository.LeadRepository;
import com.repusense.backend.repository.ReviewRepository;
import com.repusense.backend.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/leads")
@CrossOrigin(origins = "*")
public class LeadController {

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private GeminiService geminiService;

    @PostMapping
    public ResponseEntity<Lead> createLead(@RequestBody Lead lead) {
        return ResponseEntity.ok(leadRepository.save(lead));
    }

    @GetMapping
    public ResponseEntity<Page<Lead>> getAllLeads(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Specification<Lead> spec =
                (root, query, cb) -> cb.conjunction();

        if (city != null && !city.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(
                            cb.lower(root.get("business").get("city")),
                            "%" + city.toLowerCase() + "%"
                    )
            );
        }

        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("status"), status)
            );
        }

        return ResponseEntity.ok(leadRepository.findAll(spec, pageable));
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<Map<String, Object>> analyzeLead(
            @PathVariable Long id
    ) {
        Lead lead = leadRepository.findById(id).orElse(null);

        if (lead == null) {
            return ResponseEntity.notFound().build();
        }

        Business business = lead.getBusiness();

        if (business == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "El lead no tiene un negocio asociado.")
            );
        }

        List<Review> reviews =
                reviewRepository.findByBusinessId(business.getId());

        String reviewsText = reviews.stream()
                .map(Review::getText)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("\n"));

        StringBuilder information = new StringBuilder();
        information.append("Nombre: ").append(valueOrUnknown(business.getName()));
        information.append("\nCiudad: ").append(valueOrUnknown(business.getCity()));
        information.append("\nCategoría: ").append(valueOrUnknown(business.getCategory()));
        information.append("\nDirección: ").append(valueOrUnknown(business.getAddress()));
        information.append("\nCalificación registrada: ")
                .append(business.getRating() == null
                        ? "Sin datos" : business.getRating());
        information.append("\nCantidad de reseñas registrada: ")
                .append(business.getReviewCount() == null
                        ? "Sin datos" : business.getReviewCount());
        information.append("\nSitio web: ").append(valueOrUnknown(business.getWebsite()));

        if (!reviewsText.isBlank()) {
            information.append("\n\nReseñas guardadas:\n").append(reviewsText);
        } else {
            information.append(
                    "\n\nNo hay textos de reseñas guardados. "
                    + "Realiza una evaluación preliminar únicamente con los datos "
                    + "del perfil comercial y aclara esta limitación."
            );
        }

        try {
            String analysis = geminiService.analyzeBusinessReviews(
                    business.getName(),
                    information.toString()
            );

            lead.setAiAnalysis(analysis);
            leadRepository.save(lead);

            Map<String, Object> response = new HashMap<>();
            response.put("leadId", lead.getId());
            response.put("analysis", analysis);
            response.put("saved", true);
            response.put("hasReviews", !reviewsText.isBlank());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(502).body(
                    Map.of(
                            "error",
                            e.getMessage() == null
                                    ? "No se pudo completar el análisis con Gemini."
                                    : e.getMessage()
                    )
            );
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateLeadStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> updates
    ) {
        Lead lead = leadRepository.findById(id).orElse(null);

        if (lead == null) {
            return ResponseEntity.notFound().build();
        }

        if (updates.containsKey("status")) {
            lead.setStatus(updates.get("status"));
            leadRepository.save(lead);
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportLeadsToCsv() {
        List<Lead> leads = leadRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("ID Lead,Nombre,Ciudad,Categoria,Direccion,Calificacion,")
                .append("Cantidad Resenas,Telefono,Web,Estado,Email Contacto,")
                .append("Analisis IA\n");

        for (Lead lead : leads) {
            Business business = lead.getBusiness();

            csv.append(lead.getId()).append(',')
                    .append(csvValue(business == null ? "" : business.getName())).append(',')
                    .append(csvValue(business == null ? "" : business.getCity())).append(',')
                    .append(csvValue(business == null ? "" : business.getCategory())).append(',')
                    .append(csvValue(business == null ? "" : business.getAddress())).append(',')
                    .append(csvValue(business == null || business.getRating() == null
                            ? "" : business.getRating().toString())).append(',')
                    .append(csvValue(business == null || business.getReviewCount() == null
                            ? "" : business.getReviewCount().toString())).append(',')
                    .append(csvValue(business == null ? "" : business.getPhone())).append(',')
                    .append(csvValue(business == null ? "" : business.getWebsite())).append(',')
                    .append(csvValue(lead.getStatus())).append(',')
                    .append(csvValue(lead.getContactEmail())).append(',')
                    .append(csvValue(lead.getAiAnalysis())).append('\n');
        }

        byte[] content = csv.toString().getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                MediaType.parseMediaType("text/csv; charset=UTF-8")
        );
        headers.set(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"repusense-leads.csv\""
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(content);
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "Sin datos" : value;
    }

    private String csvValue(String value) {
        if (value == null) {
            return "\"\"";
        }

        String safeValue = value.replace("\"", "\"\"");
        return "\"" + safeValue + "\"";
    }
}
