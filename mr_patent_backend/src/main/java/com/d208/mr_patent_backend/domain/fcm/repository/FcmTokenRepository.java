package com.d208.mr_patent_backend.domain.fcm.repository;

import com.d208.mr_patent_backend.domain.fcm.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Integer> {
    Optional<FcmToken> findByUserId(Integer userId);


}