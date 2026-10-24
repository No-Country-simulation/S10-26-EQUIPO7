package com.repusense.backend.service;

import com.repusense.backend.model.Lead;
import com.repusense.backend.repository.LeadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LeadService {

    @Autowired
    private LeadRepository leadRepository;

    // Obtener todos los leads registrados
    public List<Lead> getAllLeads() {
        return leadRepository.findAll();
    }

    // Buscar un lead por su ID
    public Optional<Lead> getLeadById(Long id) {
        return leadRepository.findById(id);
    }

    // Guardar o actualizar un lead
    public Lead saveLead(Lead lead) {
        return leadRepository.save(lead);
    }

    // Eliminar un lead
    public void deleteLead(Long id) {
        leadRepository.deleteById(id);
    }
}