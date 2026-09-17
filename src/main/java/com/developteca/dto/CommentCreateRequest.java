package com.developteca.dto;

import jakarta.validation.constraints.NotBlank;

public class CommentCreateRequest {

    @NotBlank(message = "El comentario no puede estar vacío")
    private String content;

    private Long parentCommentId;

    public CommentCreateRequest(){}

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(Long parentCommentId) {
        this.parentCommentId = parentCommentId;
    }
}
