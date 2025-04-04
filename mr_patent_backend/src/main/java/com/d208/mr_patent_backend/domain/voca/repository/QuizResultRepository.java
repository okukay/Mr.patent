package com.d208.mr_patent_backend.domain.voca.repository;

import com.d208.mr_patent_backend.domain.user.entity.User;
import com.d208.mr_patent_backend.domain.voca.entity.QuizResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {

    /**
     * 사용자의 특정 레벨 퀴즈 결과 조회 (최신순)
     */
    @Query("SELECT qr FROM QuizResult qr WHERE qr.user = :user AND qr.levelNumber = :levelNumber ORDER BY qr.createdAt DESC")
    List<QuizResult> findByUserAndLevelNumberOrderByCreatedAtDesc(@Param("user") User user, @Param("levelNumber") Byte levelNumber);

    /**
     * 사용자의 모든 퀴즈 결과 조회 (레벨 오름차순, 각 레벨 내에서는 최신순)
     */
    @Query("SELECT qr FROM QuizResult qr WHERE qr.user = :user ORDER BY qr.levelNumber ASC, qr.createdAt DESC")
    List<QuizResult> findByUserOrderByLevelNumberAscCreatedAtDesc(@Param("user") User user);

    /**
     * 사용자의 특정 레벨 최근 퀴즈 결과 조회
     */
    @Query("SELECT qr FROM QuizResult qr WHERE qr.user = :user AND qr.levelNumber = :levelNumber ORDER BY qr.createdAt DESC")
    Optional<QuizResult> findTopByUserAndLevelNumberOrderByCreatedAtDesc(@Param("user") User user, @Param("levelNumber") Byte levelNumber);
}