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
    public CommentResponse create(Long articleId, CommentCreateRequest request, User author) {
        // Trampa anti-spam: si el campo oculto viene relleno, es un bot.
        // Se descarta en silencio, sin error, para no darle información.
        if (request.getWebsite() != null && !request.getWebsite().isBlank()) {
            throw new ApiException("No se pudo publicar el comentario");
        }

        boolean esAnonimo = (author == null);

        if (esAnonimo && (request.getAuthorName() == null || request.getAuthorName().isBlank())) {
            throw new ApiException("El nombre es obligatorio para comentar sin cuenta");
        }

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ApiException("Artículo no encontrado"));

        Comment parentComment = null;
        if (request.getParentCommentId() != null) {
            parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ApiException("Comentario padre no encontrado"));

            if (!parentComment.getArticle().getId().equals(articleId)) {
                throw new ApiException("El comentario padre no pertenece a este articulo");
            }
        }

        Comment comment = esAnonimo
                ? new Comment(request.getContent(), article,
                request.getAuthorName().trim(), request.getAuthorEmail(), parentComment)
                : new Comment(request.getContent(), article, author, parentComment);

        // Los registrados publican al instante; los anónimos pasan por revisión.
        comment.setStatus(esAnonimo ? CommentStatus.PENDING : CommentStatus.APPROVED);

        Comment saved = commentRepository.save(comment);

        // El contador solo cuenta lo que se ve en público.
        if (comment.getStatus() == CommentStatus.APPROVED) {
            article.setCommentsCount(article.getCommentsCount() + 1);
            articleRepository.save(article);
        }

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

        boolean isAdmin = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.SUPER_ADMIN;
        // El autor puede ser null (comentario anónimo): en ese caso nadie puede
        // reclamar su propiedad, solo un admin lo borra.
        boolean isOwner = comment.getAuthor() != null
                && comment.getAuthor().getId().equals(currentUser.getId());

        if (!isOwner && !isAdmin) {
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
        // Se razona sobre el invariante "¿cuenta este comentario?" en vez de enumerar
        // transiciones: así sigue siendo correcto si mañana se añade otro estado.
        boolean contabaAntes = oldStatus == CommentStatus.APPROVED;
        boolean cuentaAhora = newStatus == CommentStatus.APPROVED;

        if (contabaAntes == cuentaAhora) return;

        Article article = comment.getArticle();
        int delta = cuentaAhora ? 1 : -1;
        article.setCommentsCount(Math.max(0, article.getCommentsCount() + delta));
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
        CommentResponse response = new CommentResponse(
                comment.getId(), comment.getContent(), null, comment.getCreatedAt());

        if (comment.getAuthor() != null) {
            response.setAuthor(new AuthorResponse(
                    comment.getAuthor().getId(),
                    comment.getAuthor().getFirstName(),
                    comment.getAuthor().getLastName()));
        } else {
            response.setAuthorName(comment.getAuthorName());
        }

        response.setStatus(comment.getStatus());
        return response;
    }
}