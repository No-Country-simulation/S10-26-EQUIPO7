
package com.repusense.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "search_run")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "La ciudad es obligatoria")
    @Column(nullable = false, length = 100)
    private String city;

    @NotBlank(message = "La categoría es obligatoria")
    @Column(nullable = false, length = 100)
    private String category;

    @NotBlank(message = "El estado de búsqueda es obligatorio")
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING";

    @PositiveOrZero
    @Column(name = "found_count")
    @Builder.Default
    private Integer foundCount = 0;

    @PositiveOrZero
    @Column(name = "saved_count")
    @Builder.Default
    private Integer savedCount = 0;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name = "search_run_business",
            joinColumns = @JoinColumn(name = "search_run_id"),
            inverseJoinColumns = @JoinColumn(name = "business_id"),
            uniqueConstraints = @UniqueConstraint(
                    columnNames = {"search_run_id", "business_id"}
            )
    )
    @Builder.Default
    private Set<Business> businesses = new LinkedHashSet<>();

    @PrePersist
    public void prePersist() {
        if (this.startedAt == null) {
            this.startedAt = OffsetDateTime.now();
        }

        if (this.status == null) {
            this.status = "PENDING";
        }

        if (this.foundCount == null) {
            this.foundCount = 0;
        }

        if (this.savedCount == null) {
            this.savedCount = 0;
        }

        if (this.businesses == null) {
            this.businesses = new LinkedHashSet<>();
        }
    }
}
