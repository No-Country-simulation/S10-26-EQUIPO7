package com.repusense.backend.repository;

import com.repusense.backend.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    // Buscar todas las reseñas asociadas a un negocio específico
    List<Review> findByBusinessId(Long businessId);
}