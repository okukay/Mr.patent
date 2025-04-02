package com.d208.mr_patent_backend.domain.user.repository;

import com.d208.mr_patent_backend.domain.user.entity.Expert;
import com.d208.mr_patent_backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpertRepository extends JpaRepository<Expert, Integer> {
    Optional<Expert> findByUser(User user);
    List<Expert> findByExpertStatus(Integer status);
}