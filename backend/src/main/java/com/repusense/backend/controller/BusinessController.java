package com.repusense.backend.controller;

import com.repusense.backend.model.Business;
import com.repusense.backend.service.BusinessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/businesses")
@CrossOrigin(origins = "*")
public class BusinessController {

    @Autowired
    private BusinessService businessService;

    @GetMapping
    public List<Business> getAllBusinesses() {
        return businessService.getAllBusinesses();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Business> getBusinessById(@PathVariable Long id) {
        return businessService.getBusinessById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Business> createBusiness(@RequestBody Business business) {
        Business savedBusiness = businessService.saveBusiness(business);
        return new ResponseEntity<>(savedBusiness, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Business> updateBusiness(@PathVariable Long id, @RequestBody Business businessDetails) {
        try {
            Business updatedBusiness = businessService.updateBusiness(id, businessDetails);
            return ResponseEntity.ok(updatedBusiness);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBusiness(@PathVariable Long id) {
        if (businessService.getBusinessById(id).isPresent()) {
            businessService.deleteBusiness(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}