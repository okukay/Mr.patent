package com.d208.mr_patent_backend.domain.voca.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "word")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "word_id")
    private Long id;

    @Column(name = "word_name", nullable = false, length = 30)
    private String name;

    @Column(name = "word_mean", nullable = false, length = 255)
    private String mean;

    @Column(name = "level_number", nullable = false)
    private Byte level;

    @Column(name = "word_created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "word_updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}