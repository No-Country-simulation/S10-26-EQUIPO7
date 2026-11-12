package com.repusense.backend.repository;

import com.repusense.backend.model.SearchRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchRunRepository extends JpaRepository<SearchRun, Long> {

    @EntityGraph(attributePaths = "businesses")
    Optional<SearchRun> findWithBusinessesById(Long id);

    @Modifying
    @Query(
            value = "delete from search_run_business where business_id in (:businessIds)",
            nativeQuery = true
    )
    int unlinkBusinesses(@Param("businessIds") List<Long> businessIds);

    @Modifying
    @Query(
            value = "delete from search_run_business where search_run_id = :searchRunId",
            nativeQuery = true
    )
    int unlinkBusinessesFromSearchRun(@Param("searchRunId") Long searchRunId);

    @Modifying
    @Query(value = "delete from search_run_business", nativeQuery = true)
    int unlinkAllBusinessesFromSearchRuns();
}