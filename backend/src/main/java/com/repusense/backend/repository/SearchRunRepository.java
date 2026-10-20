package com.repusense.backend.repository;

import com.repusense.backend.model.SearchRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchRunRepository extends JpaRepository<SearchRun, Long> {
    
}