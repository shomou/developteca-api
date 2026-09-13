package com.developteca.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.developteca.entity.ArticleImage;

@Repository
public interface ArticleImageRepository extends JpaRepository<ArticleImage, Long>{
    List<ArticleImage> findByArticleIdOrderByOrderIndexAsc(Long articleId);
    Optional<ArticleImage> findByArticleIdAndIsFeaturedTrue(Long articleId);
}
