
package com.repusense.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "business", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"source", "source_business_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(name = "source_business_id", nullable = false)
    private String sourceBusinessId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String category;

    private Double rating;

    @Column(name = "review_count")
    private Integer reviewCount;

    private String phone;

    private String website;

    @Column(length = 500)
    private String address;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void setUpdatedAt() {
        this.updatedAt = OffsetDateTime.now();
    }
}
