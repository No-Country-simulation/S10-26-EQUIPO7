
package com.repusense.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
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

    @NotBlank(message = "La fuente del comercio es obligatoria")
    @Column(nullable = false, length = 50)
    private String source;

    @NotBlank(message = "El identificador externo es obligatorio")
    @Column(name = "source_business_id", nullable = false, length = 255)
    private String sourceBusinessId;

    @NotBlank(message = "El nombre del comercio es obligatorio")
    @Column(nullable = false, length = 255)
    private String name;

    @NotBlank(message = "La ciudad es obligatoria")
    @Column(nullable = false, length = 100)
    private String city;

    @NotBlank(message = "La categoría es obligatoria")
    @Column(nullable = false, length = 100)
    private String category;

    @Min(value = 0, message = "La calificación no puede ser negativa")
    @Max(value = 5, message = "La calificación no puede superar 5")
    private Double rating;

    @PositiveOrZero(message = "La cantidad de reseñas no puede ser negativa")
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
