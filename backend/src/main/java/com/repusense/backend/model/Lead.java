package com.repusense.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // <-- 1. Importar esto
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "lead")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // <-- 2. Agregar esta línea
    private Business business;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "NEW";

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "ai_main_problem")
    private String aiMainProblem;

    @Column(name = "ai_email_subject", columnDefinition = "TEXT")
    private String aiEmailSubject;

    @Column(name = "ai_email_opening", columnDefinition = "TEXT")
    private String aiEmailOpening;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // Métodos explícitos para asegurar compatibilidad total sin depender de Lombok
    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return this.status;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
        if (this.status == null) {
            this.status = "NEW";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}