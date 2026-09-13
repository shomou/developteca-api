package com.developteca.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.developteca.entity.ArticleStatus;

public class ArticleSummaryResponse {

    private Long id;
    private String title;
    private String slug;
    private String excerpt;
    private AuthorResponse author;
    private CategoryResponse category;
    private ArticleImageResponse featuredImage;
    private BigDecimal averageRating;
    private Integer commentsCount;
    private Integer viewsCount;
    private ArticleStatus status;
    private LocalDateTime publishedAt;

    public ArticleSummaryResponse() {

    }

    public ArticleSummaryResponse(Long id, String title, String slug, String excerpt, AuthorResponse author,
                                   CategoryResponse category, ArticleImageResponse featuredImage,
                                   BigDecimal averageRating, Integer commentsCount, Integer viewsCount,
                                   ArticleStatus status, LocalDateTime publishedAt) {
        this.id = id;
        this.title = title;
        this.slug = slug;
        this.excerpt = excerpt;
        this.author = author;
        this.category = category;
        this.featuredImage = featuredImage;
        this.averageRating = averageRating;
        this.commentsCount = commentsCount;
        this.viewsCount = viewsCount;
        this.status = status;
        this.publishedAt = publishedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    public AuthorResponse getAuthor() {
        return author;
    }

    public void setAuthor(AuthorResponse author) {
        this.author = author;
    }

    public CategoryResponse getCategory() {
        return category;
    }

    public void setCategory(CategoryResponse category) {
        this.category = category;
    }

    public ArticleImageResponse getFeaturedImage() {
        return featuredImage;
    }

    public void setFeaturedImage(ArticleImageResponse featuredImage) {
        this.featuredImage = featuredImage;
    }

    public BigDecimal getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(BigDecimal averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(Integer commentsCount) {
        this.commentsCount = commentsCount;
    }

    public Integer getViewsCount() {
        return viewsCount;
    }

    public void setViewsCount(Integer viewsCount) {
        this.viewsCount = viewsCount;
    }

    public ArticleStatus getStatus() {
        return status;
    }

    public void setStatus(ArticleStatus status) {
        this.status = status;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    

}
