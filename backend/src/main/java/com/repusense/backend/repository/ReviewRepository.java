package com.repusense.backend.repository;

import com.repusense.backend.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByBusinessId(Long businessId);

    @Modifying
    @Query("delete from Review review where review.business.id in :businessIds")
    int deleteAllByBusinessIds(@Param("businessIds") List<Long> businessIds);
}