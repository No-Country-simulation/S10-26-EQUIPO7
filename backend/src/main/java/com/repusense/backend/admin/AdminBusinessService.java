package com.repusense.backend.admin;

import com.repusense.backend.admin.dto.AdminBusinessResponse;
import com.repusense.backend.admin.dto.DeleteBusinessesResponse;
import com.repusense.backend.repository.BusinessRepository;
import com.repusense.backend.service.BusinessDeletionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminBusinessService {

    private final BusinessRepository businessRepository;
    private final BusinessDeletionService businessDeletionService;

    public AdminBusinessService(
            BusinessRepository businessRepository,
            BusinessDeletionService businessDeletionService) {
        this.businessRepository = businessRepository;
        this.businessDeletionService = businessDeletionService;
    }

    @Transactional(readOnly = true)
    public List<AdminBusinessResponse> getBusinesses() {
        return businessRepository.findAllForAdministration();
    }

    public DeleteBusinessesResponse deleteBusiness(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "El ID del comercio debe ser un número positivo."
            );
        }
        if (!businessRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No se encontró el comercio con ID: " + id
            );
        }

        businessDeletionService.deleteBusinesses(List.of(id));
        return new DeleteBusinessesResponse(
                1,
                List.of(id),
                "El comercio fue eliminado correctamente."
        );
    }

    public DeleteBusinessesResponse deleteBusinesses(List<Long> ids) {
        List<Long> deletedIds = businessDeletionService.deleteBusinesses(ids);
        return new DeleteBusinessesResponse(
                deletedIds.size(),
                deletedIds,
                "Los comercios seleccionados fueron eliminados correctamente."
        );
    }
}
