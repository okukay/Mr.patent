package com.d208.mr_patent_backend.domain.voca.repository;

import com.d208.mr_patent_backend.domain.user.entity.User;
import com.d208.mr_patent_backend.domain.voca.entity.Bookmark;
import com.d208.mr_patent_backend.domain.voca.entity.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    /**
     * 사용자와 단어로 북마크 존재 여부 확인
     */
    boolean existsByUserAndWord(User user, Word word);

    /**
     * 사용자의 북마크 목록 조회 (최신순)
     */
    @Query("SELECT b FROM Bookmark b WHERE b.user = :user ORDER BY b.id DESC")
    List<Bookmark> findByUserOrderByIdDesc(@Param("user") User user);

    /**
     * 사용자의 특정 레벨 북마크 목록 조회 (최신순)
     */
    @Query("SELECT b FROM Bookmark b JOIN b.word w WHERE b.user = :user AND w.level = :level ORDER BY b.id DESC")
    List<Bookmark> findByUserAndWordLevelOrderByIdDesc(@Param("user") User user, @Param("level") Byte level);

    /**
     * 사용자의 특정 레벨 북마크 개수 조회
     */
    @Query("SELECT COUNT(b) FROM Bookmark b JOIN b.word w WHERE b.user = :user AND w.level = :level")
    long countByUserAndWordLevel(@Param("user") User user, @Param("level") Byte level);

    /**
     * 사용자의 레벨별 북마크 개수 조회
     */
    @Query("SELECT w.level, COUNT(b) FROM Bookmark b JOIN b.word w WHERE b.user = :user GROUP BY w.level")
    List<Object[]> countByUserGroupByWordLevel(@Param("user") User user);

    /**
     * 사용자와 단어로 북마크 삭제
     */
    void deleteByUserAndWord(User user, Word word);

    /**
     * 사용자와 단어로 북마크 조회
     */
    @Query("SELECT b FROM Bookmark b WHERE b.user = :user AND b.word = :word")
    Bookmark findByUserAndWord(@Param("user") User user, @Param("word") Word word);
}