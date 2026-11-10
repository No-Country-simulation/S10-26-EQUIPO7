
package com.repusense.backend.repository;

import com.repusense.backend.model.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {

    boolean existsByBusiness_Id(Long businessId);

    @Query("select lead from Lead lead join fetch lead.business")
    List<Lead> findAllWithBusiness();

    @Query("""
            select distinct lead
            from Lead lead
            join fetch lead.business business
            where exists (
                select searchRun.id
                from SearchRun searchRun
                join searchRun.businesses searchBusiness
                where searchRun.id = :searchRunId
                  and searchBusiness.id = business.id
            )
            """)
    List<Lead> findAllBySearchRunId(@Param("searchRunId") Long searchRunId);

    @Query("""
            select lead
            from Lead lead
            join fetch lead.business business
            where lower(business.city) = lower(:city)
              and lower(business.category) = lower(:category)
            """)
    List<Lead> findAllByBusinessCityAndCategory(
            @Param("city") String city,
            @Param("category") String category);
}
