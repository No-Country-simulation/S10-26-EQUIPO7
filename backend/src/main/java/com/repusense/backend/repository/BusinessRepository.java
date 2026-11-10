package com.repusense.backend.repository;

import com.repusense.backend.model.Business;
import com.repusense.backend.admin.dto.AdminBusinessResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessRepository extends JpaRepository<Business, Long> {
    Optional<Business> findBySourceAndSourceBusinessId(String source, String sourceBusinessId);

    @Query("""
            select new com.repusense.backend.admin.dto.AdminBusinessResponse(
                business.id,
                business.name,
                business.city,
                business.category,
                business.address,
                business.rating,
                business.reviewCount,
                (
                    select lead.status
                    from Lead lead
                    where lead.business.id = business.id
                      and lead.id = (
                          select max(latestLead.id)
                          from Lead latestLead
                          where latestLead.business.id = business.id
                      )
                )
            )
            from Business business
            order by lower(business.name), business.id
            """)
    List<AdminBusinessResponse> findAllForAdministration();
}