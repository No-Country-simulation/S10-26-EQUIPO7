package com.repusense.backend.service;

import com.repusense.backend.model.Business;
import com.repusense.backend.repository.BusinessRepository;
import com.repusense.backend.repository.LeadRepository;
import com.repusense.backend.repository.ReviewRepository;
import com.repusense.backend.repository.SearchRunRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class BusinessDeletionService {

    private final BusinessRepository businessRepository;
    private final LeadRepository leadRepository;
    private final ReviewRepository reviewRepository;
    private final SearchRunRepository searchRunRepository;

    public BusinessDeletionService(
            BusinessRepository businessRepository,
            LeadRepository leadRepository,
            ReviewRepository reviewRepository,
            SearchRunRepository searchRunRepository) {
        this.businessRepository = businessRepository;
        this.leadRepository = leadRepository;
        this.reviewRepository = reviewRepository;
        this.searchRunRepository = searchRunRepository;
    }

    @Transactional
    public List<Long> deleteBusinesses(List<Long> businessIds) {
        if (businessIds == null || businessIds.isEmpty()
                || businessIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException(
                    "La selección debe contener IDs positivos y no vacíos."
            );
        }

        Set<Long> uniqueIds = new LinkedHashSet<>(businessIds);
        List<Long> idsToDelete = new ArrayList<>(uniqueIds);
        List<Business> businesses = businessRepository.findAllById(idsToDelete);
        if (businesses.size() != idsToDelete.size()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Uno o más comercios seleccionados no existen. No se eliminó ningún registro."
            );
        }

        searchRunRepository.unlinkBusinesses(idsToDelete);
        reviewRepository.deleteAllByBusinessIds(idsToDelete);
        leadRepository.deleteAllByBusinessIds(idsToDelete);
        businessRepository.deleteAllByIdInBatch(idsToDelete);

        return idsToDelete;
    }
}
