package com.repusense.backend.service;

import com.repusense.backend.model.Business;
import com.repusense.backend.repository.BusinessRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BusinessService {

    @Autowired
    private BusinessRepository businessRepository;

    public List<Business> getAllBusinesses() {
        return businessRepository.findAll();
    }

    public Optional<Business> getBusinessById(Long id) {
        return businessRepository.findById(id);
    }

    // Este es el método que está buscando el BusinessController
    public Business saveBusiness(Business business) {
        return businessRepository.save(business);
    }

    public Business updateBusiness(Long id, Business businessDetails) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Business not found with id: " + id));
        
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