
package com.repusense.backend.controller;

import com.repusense.backend.model.Business;
import com.repusense.backend.service.BusinessService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/businesses")
@CrossOrigin(origins = "*")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @GetMapping
    public ResponseEntity<List<Business>> getAllBusinesses() {
        return ResponseEntity.ok(
                businessService.getAllBusinesses()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Business> getBusinessById(
            @PathVariable Long id) {

        validateId(id);

        Business business = businessService.getBusinessById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró el comercio con ID: " + id
                ));

        return ResponseEntity.ok(business);
    }

    @PostMapping
    public ResponseEntity<Business> createBusiness(
            @RequestBody(required = false) Business business) {

        if (business == null) {
            throw new IllegalArgumentException(
                    "Los datos del comercio son obligatorios."
            );
        }

        Business savedBusiness =
                businessService.saveBusiness(business);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedBusiness);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Business> updateBusiness(
            @PathVariable Long id,
            @RequestBody(required = false) Business businessDetails) {

        validateId(id);

        if (businessDetails == null) {
            throw new IllegalArgumentException(
                    "Los datos para actualizar el comercio son obligatorios."
            );
        }

        Business updatedBusiness =
                businessService.updateBusiness(id, businessDetails);

        return ResponseEntity.ok(updatedBusiness);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBusiness(
            @PathVariable Long id) {

        validateId(id);

        if (businessService.getBusinessById(id).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No se encontró el comercio con ID: " + id
            );
        }

        businessService.deleteBusiness(id);

        return ResponseEntity.noContent().build();
    }

    private void validateId(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "El ID del comercio debe ser un número positivo."
            );
        }
    }
}
