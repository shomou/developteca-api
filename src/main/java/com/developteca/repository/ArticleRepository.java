package com.developteca.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.developteca.entity.Article;
import com.developteca.entity.ArticleStatus;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long>{

    Optional<Article> findBySlug(String slug);

    boolean existsBySlug(String slug);

    // Listado público: solo los articulos PUBLISHED, opcionalmente filtrados por categoria y búsqueda
    @Query("SELECT a FROM Article a WHERE a.status = :status " +
           "AND (:categorySlug IS NULL OR a.category.slug = :categorySlug) " +
           "AND (:search IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<Article> findByPublishedArticles(
        @Param("status") ArticleStatus status,
            @Param("categorySlug") String categorySlug,
            @Param("search") String search,
            Pageable pageable
    );

    Page<Article> findByAuthor(Long authorId, Pageable pageable);

    long countByStatus(ArticleStatus status);

    @Query("SELECT COALESCE(SUM(a.viewsCount),0) FROM Article a")
    long sumTotalViews();

    @Query("SELECT COALESCE(SUM(a.commentsCount),0) FROM Article a")
    long sumTotalComments();

    @Query("SELECT COALESCE(AVG(a.averageRating),0) FROM Article a WHERE a.averageRating > 0")
    double averageRatingOverall();
}
