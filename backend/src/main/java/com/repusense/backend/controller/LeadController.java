package com.repusense.backend.controller;
import org.springframework.web.bind.annotation.PostMapping;
import com.repusense.backend.model.Lead;
import com.repusense.backend.repository.LeadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.Map;

@RestController
@RequestMapping("/api/v1/leads")
@CrossOrigin(origins = "*") // Permite la comunicación con el Frontend en React
public class LeadController {

    @Autowired
    private LeadRepository leadRepository;

    // Endpoint para guardar/crear un nuevo Lead desde el scraper/IA
    @PostMapping
    public ResponseEntity<Lead> createLead(@RequestBody Lead lead) {
        Lead savedLead = leadRepository.save(lead);
        return ResponseEntity.ok(savedLead);
    }

    // Endpoint paginado con filtros opcionales por ciudad y estado
    @GetMapping
    public ResponseEntity<Page<Lead>> getAllLeads(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        // Condición inicial segura sin ambigüedades
        Specification<Lead> spec = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (city != null && !city.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get("business").get("city")), "%" + city.toLowerCase() + "%")
            );
        }

        if (status != null && !status.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), status)
            );
        }

        Page<Lead> leads = leadRepository.findAll(spec, pageable);
        return ResponseEntity.ok(leads);
    }

    // Endpoint para actualizar el estado comercial del Lead (ej. a CONTACTED)
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateLeadStatus(@PathVariable Long id, @RequestBody Map<String, String> updates) {
        return leadRepository.findById(id).map(lead -> {
            if (updates.containsKey("status")) {
                lead.setStatus(updates.get("status"));
                leadRepository.save(lead);
            }
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}