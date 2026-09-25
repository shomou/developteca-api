package com.developteca.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.developteca.dto.CommentCreateRequest;
import com.developteca.dto.CommentModerateRequest;
import com.developteca.dto.CommentResponse;
import com.developteca.entity.User;
import com.developteca.service.CommentService;
import com.developteca.util.ApiResponse;
import com.developteca.util.SecurityUtil;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/articles/{articleId}/comments")
public class CommentController {

    private final CommentService commentService;
    private final SecurityUtil securityUtil;

    public CommentController(CommentService commentService, SecurityUtil securityUtil) {
        this.commentService = commentService;
        this.securityUtil = securityUtil;
    }

    // ============ LISTAR COMENTARIOS (público) =============
    @GetMapping
    public ResponseEntity<?> list(
            @PathVariable Long articleId,
            @RequestParam(defaultValue = "false") boolean includeRejected) {
        try {
            User currentUser = securityUtil.getCurrentUserOrNull();
            List<CommentResponse> comments = commentService.getTreeByArticle(articleId, includeRejected, currentUser);
            return ResponseEntity.ok(new ApiResponse(true, "Comentarios obtenidos", comments));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ============ CREAR COMENTARIO (requiere autenticación) =============
    @PostMapping
    public ResponseEntity<?> create(@PathVariable Long articleId, @Valid @RequestBody CommentCreateRequest request) {
        try {
            // Devuelve null si no hay sesión, en vez de lanzar ClassCastException.
            User currentUser = securityUtil.getCurrentUserOrNull();
            CommentResponse created = commentService.create(articleId, request, currentUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    new ApiResponse(true, "Comentario creado", created));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ============ MODERAR COMENTARIO (solo admin) =============
    @PutMapping("/{commentId}/moderate")
    public ResponseEntity<?> moderate(@PathVariable Long articleId, @PathVariable Long commentId,
                                      @Valid @RequestBody CommentModerateRequest request) {
        try {
            User currentUser = securityUtil.getCurrentUser();
            CommentResponse updated = commentService.moderate(commentId, request.getStatus(), currentUser);
            return ResponseEntity.ok(new ApiResponse(true, "Comentario moderado", updated));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ============ ELIMINAR COMENTARIO (autor o admin) =============
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> delete(@PathVariable Long articleId, @PathVariable Long commentId) {
        try {
            User currentUser = securityUtil.getCurrentUser();
            commentService.delete(commentId, currentUser);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ApiResponse(false, e.getMessage(), null));
        }
    }

}
