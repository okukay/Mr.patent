package com.d208.mr_patent_backend.domain.voca.repository;

import com.d208.mr_patent_backend.domain.user.entity.User;
import com.d208.mr_patent_backend.domain.voca.entity.UserLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserLevelRepository extends JpaRepository<UserLevel, Long> {

    /**
     * 사용자의 모든 레벨 정보 조회 (레벨 번호 오름차순)
     */
    List<UserLevel> findByUserOrderByLevelNumberAsc(User user);

    /**
     * 사용자의 특정 레벨 정보 조회
     */
    Optional<UserLevel> findByUserAndLevelNumber(User user, Byte levelNumber);

    /**
     * 사용자의 특정 레벨이 통과 상태인지 확인
     */
    boolean existsByUserAndLevelNumberAndIsPassed(User user, Byte levelNumber, Byte isPassed);
}