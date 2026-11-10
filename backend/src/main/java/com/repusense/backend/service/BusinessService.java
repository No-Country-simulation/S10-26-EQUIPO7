package com.repusense.backend.service;

import com.repusense.backend.model.Business;
import com.repusense.backend.repository.BusinessRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final BusinessDeletionService businessDeletionService;

    public BusinessService(
            BusinessRepository businessRepository,
            BusinessDeletionService businessDeletionService) {
        this.businessRepository = businessRepository;
        this.businessDeletionService = businessDeletionService;
    }

    public List<Business> getAllBusinesses() {
        return businessRepository.findAll();
    }

    public Optional<Business> getBusinessById(Long id) {
        validateId(id);
        return businessRepository.findById(id);
    }

    public Business saveBusiness(Business business) {
        if (business == null) {
            throw new IllegalArgumentException("Los datos del comercio son obligatorios");
        }

        return businessRepository.save(business);
    }

    public Business updateBusiness(Long id, Business businessDetails) {
        validateId(id);

        if (businessDetails == null) {
            throw new IllegalArgumentException("Los datos para actualizar el comercio son obligatorios");
        }

        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró el comercio solicitado"
                ));

        business.setName(businessDetails.getName());
        business.setCity(businessDetails.getCity());
        business.setCategory(businessDetails.getCategory());
        business.setRating(businessDetails.getRating());
        business.setReviewCount(businessDetails.getReviewCount());
        business.setPhone(businessDetails.getPhone());
        business.setWebsite(businessDetails.getWebsite());

        return businessRepository.save(business);
    }

    public void deleteBusiness(Long id) {
        validateId(id);

        businessDeletionService.deleteBusinesses(List.of(id));
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID del comercio debe ser válido");
        }
    }
}