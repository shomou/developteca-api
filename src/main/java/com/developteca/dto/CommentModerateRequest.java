package com.developteca.dto;

import com.developteca.entity.CommentStatus;
import jakarta.validation.constraints.NotNull;

public class CommentModerateRequest {

    @NotNull(message = "El estado es obligatorio")
    private CommentStatus status;

    public CommentModerateRequest(){}

    public CommentStatus getStatus() {
        return status;
    }

    public void setStatus(CommentStatus status) {
        this.status = status;
    }
}
