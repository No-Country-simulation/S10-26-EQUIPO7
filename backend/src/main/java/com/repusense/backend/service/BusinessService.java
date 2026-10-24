package com.repusense.backend.service;

import com.repusense.backend.model.Business;
import com.repusense.backend.repository.BusinessRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BusinessService {

    private final BusinessRepository businessRepository;

    public BusinessService(BusinessRepository businessRepository) {
        this.businessRepository = businessRepository;
    }

    // Métodos requeridos por los controladores del equipo
    public List<Business> getAllBusinesses() {
        return businessRepository.findAll();
    }

    public List<Business> findAll() {
        return businessRepository.findAll();
    }

    public Optional<Business> getBusinessById(Long id) {
        return businessRepository.findById(id);
    }

    public Business saveBusiness(Business business) {
        return businessRepository.save(business);
    }

    public List<Business> saveAll(List<Business> businesses) {
        return businessRepository.saveAll(businesses);
    }

    public Business updateBusiness(Long id, Business businessDetails) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado con ID: " + id));
        
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
        businessRepository.deleteById(id);
    }
}