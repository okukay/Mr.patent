package com.d208.mr_patent_backend.domain.user.repository;

import com.d208.mr_patent_backend.domain.user.entity.Expert;
import com.d208.mr_patent_backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpertRepository extends JpaRepository<Expert, Integer> {
    Optional<Expert> findByUser(User user);


    Expert findByUser_UserId(Integer userId);

    @Query("SELECT DISTINCT e FROM Expert e " +
            "JOIN FETCH e.user " +
            "LEFT JOIN FETCH e.expertCategory ec " +
            "LEFT JOIN FETCH ec.category " +
            "WHERE e.expertStatus = :status")
    List<Expert> findByExpertStatus(@Param("status") Integer status);

    @Query("SELECT DISTINCT e FROM Expert e " +
            "JOIN FETCH e.user " +
            "LEFT JOIN FETCH e.expertCategory ec " +
            "LEFT JOIN FETCH ec.category " +
            "WHERE e.expertId = :expertId")
    Optional<Expert> findByIdWithDetails(@Param("expertId") Integer expertId);
}