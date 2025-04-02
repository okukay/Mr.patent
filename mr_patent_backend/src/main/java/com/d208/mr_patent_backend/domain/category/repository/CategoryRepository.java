package com.d208.mr_patent_backend.domain.category.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.d208.mr_patent_backend.domain.category.entity.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
}