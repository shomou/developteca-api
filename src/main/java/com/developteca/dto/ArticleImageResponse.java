package com.developteca.dto;

public class ArticleImageResponse {

    private Long id;
    private String imageUrl;
    private String altText;
    private Boolean isFeatured;
    private Integer orderIndex;
    
    public ArticleImageResponse() {
    }

    public ArticleImageResponse(Long id, String imageUrl, String altText, Boolean isFeatured, Integer orderIndex) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.altText = altText;
        this.isFeatured = isFeatured;
        this.orderIndex = orderIndex;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public Boolean getIsFeatured() {
        return isFeatured;
    }

    public void setIsFeatured(Boolean isFeatured) {
        this.isFeatured = isFeatured;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    

}
