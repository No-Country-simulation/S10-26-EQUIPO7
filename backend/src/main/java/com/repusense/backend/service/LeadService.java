
package com.repusense.backend.service;

import com.repusense.backend.model.Lead;
import com.repusense.backend.repository.LeadRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class LeadService {

    private final LeadRepository leadRepository;

    public LeadService(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    // Obtener todos los leads registrados
    public List<Lead> getAllLeads() {
        return leadRepository.findAll();
    }

    // Buscar un lead por su ID
    public Optional<Lead> getLeadById(Long id) {
        validateId(id);
        return leadRepository.findById(id);
    }

    // Guardar o actualizar un lead
    public Lead saveLead(Lead lead) {

        if (lead == null) {
            throw new IllegalArgumentException(
                    "Los datos del lead son obligatorios."
            );
        }

        return leadRepository.save(lead);
    }

    // Eliminar un lead
    public void deleteLead(Long id) {

        validateId(id);

        if (!leadRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No se encontró el lead con ID: " + id
            );
        }

        leadRepository.deleteById(id);
    }

    // Validar identificadores
    private void validateId(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "El ID del lead debe ser un número positivo."
            );
        }
    }
}
