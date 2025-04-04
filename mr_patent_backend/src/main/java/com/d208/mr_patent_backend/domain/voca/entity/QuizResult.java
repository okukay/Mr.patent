package com.d208.mr_patent_backend.domain.voca.entity;

import com.d208.mr_patent_backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_result")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_result_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "level_number", nullable = false)
    private Byte levelNumber;

    @Column(name = "score", nullable = false)
    private Byte score;

    @Column(name = "quiz_passed", nullable = false)
    private Byte quizPassed;

    @Column(name = "quiz_result_created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "quiz_result_updated_at", nullable = false)
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