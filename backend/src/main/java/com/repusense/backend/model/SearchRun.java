
package com.repusense.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.time.OffsetDateTime;

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

    @PositiveOrZero(message = "La cantidad de negocios encontrados no puede ser negativa")
    @Column(name = "found_count")
    @Builder.Default
    private Integer foundCount = 0;

    @PositiveOrZero(message = "La cantidad de negocios guardados no puede ser negativa")
    @Column(name = "saved_count")
    @Builder.Default
    private Integer savedCount = 0;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

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
    }
}
