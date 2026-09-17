package com.developteca.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.developteca.dto.RatingResponse;
import com.developteca.entity.Article;
import com.developteca.entity.Rating;
import com.developteca.entity.User;
import com.developteca.exception.ApiException;
import com.developteca.repository.ArticleRepository;
import com.developteca.repository.RatingRepository;

import jakarta.transaction.Transactional;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;
    private final ArticleRepository articleRepository;

    public RatingService(RatingRepository ratingRepository, ArticleRepository articleRepository) {
        this.ratingRepository = ratingRepository;
        this.articleRepository = articleRepository;
    }

    // ============= CREAR O ACTUALIZAR MI RATING (upsert) =============
    @Transactional
    public RatingResponse upsert(Long articleId, Integer value, User currentUser) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ApiException("Artículo no encontrado"));

        Rating rating = ratingRepository.findByArticleIdAndUserId(articleId, currentUser.getId())
                .orElse(new Rating(article, currentUser, value));

        rating.setValue(value);
        ratingRepository.save(rating);

        recalculateAverage(article);

        return new RatingResponse(article.getAverageRating().doubleValue(), value);
    }

    // ============= OBTENER MI RATING ACTUAL =============
    public RatingResponse getMyRating(Long articleId, User currentUser) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ApiException("Artículo no encontrado"));

        Integer myValue = ratingRepository.findByArticleIdAndUserId(articleId, currentUser.getId())
                .map(Rating::getValue)
                .orElse(null);

        return new RatingResponse(article.getAverageRating().doubleValue(), myValue);
    }

    // ============= HELPERS PRIVADOS =============

    private void recalculateAverage(Article article) {
        double avg = ratingRepository.averageByArticleId(article.getId());
        BigDecimal rounded = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP);
        article.setAverageRating(rounded);
        articleRepository.save(article);
    }
}