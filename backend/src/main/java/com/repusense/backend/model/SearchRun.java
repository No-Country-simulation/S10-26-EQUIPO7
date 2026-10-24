package com.repusense.backend.model;

import jakarta.persistence.*;
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

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "found_count")
    @Builder.Default
    private Integer foundCount = 0;

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
    }
}