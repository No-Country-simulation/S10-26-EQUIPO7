
package com.repusense.backend.controller;

import com.repusense.backend.model.Business;
import com.repusense.backend.model.Lead;
import com.repusense.backend.model.SearchRun;
import com.repusense.backend.repository.BusinessRepository;
import com.repusense.backend.repository.LeadRepository;
import com.repusense.backend.repository.SearchRunRepository;
import com.repusense.backend.service.GoogleMapsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/searches")
@CrossOrigin(origins = "*")
public class SearchRunController {

    private final SearchRunRepository searchRunRepository;
    private final BusinessRepository businessRepository;
    private final LeadRepository leadRepository;
    private final GoogleMapsService googleMapsService;

    public SearchRunController(
            SearchRunRepository searchRunRepository,
            BusinessRepository businessRepository,
            LeadRepository leadRepository,
            GoogleMapsService googleMapsService) {
        this.searchRunRepository = searchRunRepository;
        this.businessRepository = businessRepository;
        this.leadRepository = leadRepository;
        this.googleMapsService = googleMapsService;
    }

    @PostMapping
    public ResponseEntity<?> createSearch(@RequestBody SearchRun searchRun) {
        if (searchRun.getCity() == null
                || searchRun.getCity().isBlank()
                || searchRun.getCategory() == null
                || searchRun.getCategory().isBlank()) {
            return ResponseEntity.badRequest()
                    .body("La ciudad y el rubro son obligatorios.");
        }

        searchRun.setCity(searchRun.getCity().trim());
        searchRun.setCategory(searchRun.getCategory().trim());
        searchRun.setStatus("PENDING");
        searchRun.setStartedAt(OffsetDateTime.now());
        searchRun.setFinishedAt(null);
        searchRun.setFoundCount(0);
        searchRun.setSavedCount(0);

        SearchRun savedRun = searchRunRepository.save(searchRun);

        try {
            List<Business> businesses =
                    googleMapsService.fetchBusinessesByCityAndCategory(
                            savedRun.getCity(),
                            savedRun.getCategory());

            if (businesses == null) {
                throw new IllegalStateException(
                        "La fuente de negocios no devolvió una respuesta válida.");
            }

            int savedCount = 0;

            for (Business incoming : businesses) {
                if (!isValid(incoming)) {
                    continue;
                }

                String source = incoming.getSource().trim();
                String sourceId = incoming.getSourceBusinessId().trim();

                Optional<Business> existing =
                        businessRepository.findBySourceAndSourceBusinessId(
                                source, sourceId);

                Business business;

                if (existing.isPresent()) {
                    business = existing.get();

                    business.setName(incoming.getName());
                    business.setCity(incoming.getCity());
                    business.setCategory(incoming.getCategory());

                    if (hasText(incoming.getAddress())) {
                        business.setAddress(incoming.getAddress());
                    }

                    if (hasText(incoming.getPhone())) {
                        business.setPhone(incoming.getPhone());
                    }

                    if (hasText(incoming.getWebsite())) {
                        business.setWebsite(incoming.getWebsite());
                    }

                    if (incoming.getRating() != null) {
                        business.setRating(incoming.getRating());
                    }

                    if (incoming.getReviewCount() != null) {
                        business.setReviewCount(incoming.getReviewCount());
                    }
                } else {
                    business = incoming;
                    business.setSource(source);
                    business.setSourceBusinessId(sourceId);
                }

                Business savedBusiness = businessRepository.save(business);

                if (!leadRepository.existsByBusiness_Id(savedBusiness.getId())) {
                    Lead lead = new Lead();
                    lead.setBusiness(savedBusiness);
                    lead.setStatus("NEW");
                    leadRepository.save(lead);
                }

                savedCount++;
            }

            savedRun.setFoundCount(businesses.size());
            savedRun.setSavedCount(savedCount);
            savedRun.setStatus("COMPLETED");
            savedRun.setFinishedAt(OffsetDateTime.now());

            return ResponseEntity.ok(searchRunRepository.save(savedRun));

        } catch (Exception e) {
            savedRun.setStatus("FAILED");
            savedRun.setFinishedAt(OffsetDateTime.now());
            searchRunRepository.save(savedRun);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al ejecutar la búsqueda: " + e.getMessage());
        }
    }

    private boolean isValid(Business business) {
        return business != null
                && hasText(business.getSource())
                && hasText(business.getSourceBusinessId())
                && hasText(business.getName())
                && hasText(business.getCity())
                && hasText(business.getCategory());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @GetMapping
    public ResponseEntity<List<SearchRun>> getAllSearches() {
        return ResponseEntity.ok(searchRunRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SearchRun> getSearchStatus(@PathVariable Long id) {
        return searchRunRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
