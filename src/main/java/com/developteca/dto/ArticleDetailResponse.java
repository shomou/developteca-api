package com.developteca.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.developteca.entity.ArticleStatus;

public class ArticleDetailResponse {

    private Long id;
    private String title;
    private String slug;
    private String content;
    private AuthorResponse author;
    private CategoryResponse category;
    private List<ArticleImageResponse> images;
    private BigDecimal averageRating;
    private Integer commentsCount;
    private Integer viewsCount;
    private ArticleStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    
    public ArticleDetailResponse() {
    }

    public ArticleDetailResponse(Long id, String title, String slug, String content, AuthorResponse author,
            CategoryResponse category, List<ArticleImageResponse> images, BigDecimal averageRating,
            Integer commentsCount, Integer viewsCount, ArticleStatus status, LocalDateTime createdAt,
            LocalDateTime publishedAt) {
        this.id = id;
        this.title = title;
        this.slug = slug;
        this.content = content;
        this.author = author;
        this.category = category;
        this.images = images;
        this.averageRating = averageRating;
        this.commentsCount = commentsCount;
        this.viewsCount = viewsCount;
        this.status = status;
        this.createdAt = createdAt;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public List<ArticleImageResponse> getImages() {
        return images;
    }

    public void setImages(List<ArticleImageResponse> images) {
        this.images = images;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    

    

}
