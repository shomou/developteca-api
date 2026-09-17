package com.developteca.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.developteca.entity.Rating;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByArticleIdAndUserId(Long articleId, Long userId);

    @Query("SELECT COALESCE(AVG(r.value), 0) FROM Rating r WHERE r.article.id = :articleId")
    double averageByArticleId(@Param("articleId") Long articleId);
}