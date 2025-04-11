package com.d208.mr_patent_backend.domain.voca.repository;

import com.d208.mr_patent_backend.domain.voca.entity.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WordRepository extends JpaRepository<Word, Long> {

    /**
     * 특정 레벨의 단어 목록 조회 (이름 오름차순)
     */
    @Query("SELECT w FROM Word w WHERE w.level = :level ORDER BY w.name ASC")
    List<Word> findByLevelOrderByNameAsc(@Param("level") Byte level);

    /**
     * 단어 이름으로 조회
     */
    Word findByName(String name);

    @Query("SELECT w FROM Word w WHERE w.level = :level ORDER BY w.id ASC")
    List<Word> findByLevelOrderByWordIdAsc(@Param("level") Byte level);

    long countByLevel(byte level);
}