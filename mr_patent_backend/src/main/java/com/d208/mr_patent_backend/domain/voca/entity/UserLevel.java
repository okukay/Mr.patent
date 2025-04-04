package com.d208.mr_patent_backend.domain.voca.entity;

import com.d208.mr_patent_backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_level")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_level_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "level_number", nullable = false)
    private Byte levelNumber;

    @Column(name = "best_score", nullable = false)
    private Byte bestScore;

    @Column(name = "is_passed", nullable = false)
    private Byte isPassed;  // 0: 미통과, 1: 통과

    /**
     * 점수 업데이트
     */
    public void updateScore(Byte score) {
        this.bestScore = score;
    }

    /**
     * 레벨 통과 처리
     */
    public void pass() {
        this.isPassed = 1;
    }
}