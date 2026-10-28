
package com.repusense.backend.controller;

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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Map<String, String>> analyzeLeadReviews(
            @PathVariable Long id
    ) {
        Lead lead = leadRepository.findById(id).orElse(null);

        if (lead == null) {
            return ResponseEntity.notFound().build();
        }

        if (lead.getBusiness() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El lead no tiene un negocio asociado."));
        }

        String businessName = lead.getBusiness().getName();
        Long businessId = lead.getBusiness().getId();

        List<Review> reviews = reviewRepository.findByBusinessId(businessId);

        String reviewsText = reviews.stream()
                .map(Review::getText)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("\n"));

        if (reviewsText.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "Este negocio no tiene reseñas guardadas. No se realizó el análisis."
                    ));
        }

        String analysisResult =
                geminiService.analyzeBusinessReviews(businessName, reviewsText);

        Map<String, String> response = new HashMap<>();
        response.put("analysis", analysisResult);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateLeadStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> updates
    ) {
        return leadRepository.findById(id).map(lead -> {
            if (updates.containsKey("status")) {
                lead.setStatus(updates.get("status"));
                leadRepository.save(lead);
            }

            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
