package com.repusense.backend.repository;

import com.repusense.backend.model.Business;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BusinessRepository extends JpaRepository<Business, Long> {
    Optional<Business> findBySourceAndSourceBusinessId(String source, String sourceBusinessId);
}