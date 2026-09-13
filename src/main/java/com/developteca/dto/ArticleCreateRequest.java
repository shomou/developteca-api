package com.developteca.dto;

import com.developteca.entity.ArticleStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ArticleCreateRequest {

     @NotBlank(message = "El título no puede estar vacío")
    @Size(min = 3, max = 255, message = "El título debe tener entre 3 y 255 caracteres")
    private String title;

    @NotBlank(message = "El contenido no puede estar vacío")
    private String content;

    @NotNull(message = "La categoría es obligatoria")
    private Long categoryId;

    private ArticleStatus status = ArticleStatus.DRAFT;

    public ArticleCreateRequest(){

    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public ArticleStatus getStatus() {
        return status;
    }

    public void setStatus(ArticleStatus status) {
        this.status = status;
    }

    

}
