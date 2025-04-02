package com.d208.mr_patent_backend.domain.user.repository;

import com.d208.mr_patent_backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    // 이메일 중복 체크를 위한 메서드
    boolean existsByUserEmail(String email);

    // 이메일로 유저 찾기
    Optional<User> findByUserEmail(String email);

    Optional<User> findByUserRefreshToken(String refreshToken);

    Optional<User> findById(Long userId);
}
