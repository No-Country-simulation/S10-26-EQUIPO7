
package com.repusense.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "review")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El comercio asociado es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(name = "source_review_id")
    private String sourceReviewId;

    @NotBlank(message = "El texto de la reseña es obligatorio")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    @Min(value = 0, message = "La calificación no puede ser negativa")
    @Max(value = 5, message = "La calificación no puede superar 5")
    private Double rating;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "fetched_at")
    private OffsetDateTime fetchedAt;

    @PrePersist
    public void prePersist() {
        if (this.fetchedAt == null) {
            this.fetchedAt = OffsetDateTime.now();
        }
    }
}
