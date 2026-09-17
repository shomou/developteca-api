package com.developteca.repository;

import com.developteca.entity.Comment;
import com.developteca.entity.CommentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByArticleIdAndStatusOrderByCreatedAtAsc(Long articleId, CommentStatus status);

    List<Comment> findByArticleIdOrderByCreatedAtAsc(Long articleId);
}
