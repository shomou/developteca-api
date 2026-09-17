package com.developteca.service;

import com.developteca.dto.AuthorResponse;
import com.developteca.dto.CommentCreateRequest;
import com.developteca.dto.CommentResponse;
import com.developteca.entity.*;
import com.developteca.exception.ApiException;
import com.developteca.repository.ArticleRepository;
import com.developteca.repository.CommentRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;

    public CommentService(CommentRepository commentRepository, ArticleRepository articleRepository) {
        this.commentRepository = commentRepository;
        this.articleRepository = articleRepository;
    }

    // =============== CREAR COMENTARIO ===============
    @Transactional
    public CommentResponse create(Long articleId, CommentCreateRequest request, User author){
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ApiException("Artículo no encontrado"));

        Comment parentComment = null;
        if(request.getParentCommentId() != null){
            parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ApiException("Comentario padre no encontrado"));
            if(!parentComment.getArticle().getId().equals(articleId)){
                throw new ApiException("El comentario padre no pertenece a este articulo");
            }
        }

        Comment comment = new Comment(request.getContent(), article,author, parentComment);
        Comment saved = commentRepository.save(comment);

        article.setCommentsCount(article.getCommentsCount() + 1);
        articleRepository.save(article);

        return mapToResponse(saved);

    }

    // ============= LISTAR COMENTARIOS DE UN ARTICULO (árbol, público)
    public List<CommentResponse> getTreeByArticle(Long articleId, boolean includeRejected, User currentUser) {
        boolean isAdmin = currentUser != null
                && (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.SUPER_ADMIN);

        List<Comment> flat = (includeRejected && isAdmin)
                ? commentRepository.findByArticleIdOrderByCreatedAtAsc(articleId)
                : commentRepository.findByArticleIdAndStatusOrderByCreatedAtAsc(articleId, CommentStatus.APPROVED);

        return flat.stream()
                .filter(c -> c.getParentComment() == null)
                .map(root -> mapToTreeResponse(root, flat))
                .collect(Collectors.toList());
    }

    // ============= MODERAR COMENTARIO (solo admin) ==========
    @Transactional
    public CommentResponse moderate(Long commentId, CommentStatus newStatus, User currentUser){
        checkIsAdmin(currentUser);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException("Comentario no encontrado"));

        adjustCommentsCount(comment, comment.getStatus(), newStatus);
        comment.setStatus(newStatus);
        Comment saved = commentRepository.save(comment);

        return mapToResponse(saved);
    }

    // ============= ELIMINAR COMENTARIO (autor o admin) ============
    @Transactional
    public void delete(Long commentId, User currentUser){
        Comment comment =  commentRepository.findById(commentId)
                .orElseThrow(()-> new ApiException("No tienes permiso para eliminar este comentario"));

        boolean isOwner = comment.getAuthor().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.SUPER_ADMIN;

        if(!isOwner && !isAdmin){
            throw new ApiException("No tienes permiso para eliminar este comentario");
        }

        if (comment.getStatus() == CommentStatus.APPROVED) {
            Article article = comment.getArticle();
            article.setCommentsCount(Math.max(0, article.getCommentsCount() - 1));
            articleRepository.save(article);
        }

        commentRepository.delete(comment);
    }


    // ============= HELPERS PRIVADOS =============

    private void checkIsAdmin(User currentUser) {
        boolean isAdmin = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.SUPER_ADMIN;
        if (!isAdmin) {
            throw new ApiException("Solo un administrador puede moderar comentarios");
        }
    }

    private void adjustCommentsCount(Comment comment, CommentStatus oldStatus, CommentStatus newStatus) {
        if (oldStatus == newStatus) return;

        Article article = comment.getArticle();
        if (oldStatus == CommentStatus.APPROVED && newStatus == CommentStatus.REJECTED) {
            article.setCommentsCount(Math.max(0, article.getCommentsCount() - 1));
        } else if (oldStatus == CommentStatus.REJECTED && newStatus == CommentStatus.APPROVED) {
            article.setCommentsCount(article.getCommentsCount() + 1);
        }
        articleRepository.save(article);
    }

    private CommentResponse mapToTreeResponse(Comment comment, List<Comment> allComments) {
        CommentResponse response = mapToResponse(comment);

        List<CommentResponse> replies = allComments.stream()
                .filter(c -> c.getParentComment() != null && c.getParentComment().getId().equals(comment.getId()))
                .map(reply -> mapToTreeResponse(reply, allComments))
                .collect(Collectors.toList());

        response.setReplies(replies);
        return response;
    }

    private CommentResponse mapToResponse(Comment comment) {
        AuthorResponse author = new AuthorResponse(
                comment.getAuthor().getId(),
                comment.getAuthor().getFirstName(),
                comment.getAuthor().getLastName()
        );
        CommentResponse response = new CommentResponse(
                comment.getId(), comment.getContent(), author, comment.getCreatedAt());
        response.setStatus(comment.getStatus());
        return response;
    }
}