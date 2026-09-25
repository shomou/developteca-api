package com.developteca.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CommentCreateRequest {

    @NotBlank(message = "El comentario no puede estar vacío")
    private String content;

    private Long parentCommentId;

    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String authorName;

    @Email(message = "El email debe ser válido")
    @Size(max = 255)
    private String authorEmail;

    // Trampa anti-spam: el formulario lo oculta con CSS, así que una persona
    // nunca lo rellena. Un bot que complete todos los campos sí lo hará.
    private String website;

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

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }
}
