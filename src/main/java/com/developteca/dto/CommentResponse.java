package com.developteca.dto;

import com.developteca.entity.CommentStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommentResponse {

    private Long id;
    private String content;
    private AuthorResponse author;
    private LocalDateTime createdAt;
    private List<CommentResponse> replies = new ArrayList();
    private CommentStatus status;

    public CommentResponse() {}

    public CommentResponse(Long id, String content, AuthorResponse author, LocalDateTime createdAt) {
        this.id = id;
        this.content = content;
        this.author = author;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<CommentResponse> getReplies() {
        return replies;
    }

    public void setReplies(List<CommentResponse> replies) {
        this.replies = replies;
    }

    public CommentStatus getStatus() {
        return status;
    }

    public void setStatus(CommentStatus status) {
        this.status = status;
    }
}