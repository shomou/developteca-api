package com.developteca.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.developteca.dto.RatingRequest;
import com.developteca.dto.RatingResponse;
import com.developteca.entity.User;
import com.developteca.service.RatingService;
import com.developteca.util.ApiResponse;
import com.developteca.util.SecurityUtil;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/articles/{articleId}/ratings")
public class RatingController {

    private final RatingService ratingService;
    private final SecurityUtil securityUtil;

    public RatingController(RatingService ratingService, SecurityUtil securityUtil) {
        this.ratingService = ratingService;
        this.securityUtil = securityUtil;
    }

    // ============ CALIFICAR ARTICULO (crea o actualiza mi rating) =============
    @PutMapping
    public ResponseEntity<?> upsert(@PathVariable Long articleId, @Valid @RequestBody RatingRequest request) {
        try {
            User currentUser = securityUtil.getCurrentUser();
            RatingResponse response = ratingService.upsert(articleId, request.getValue(), currentUser);
            return ResponseEntity.ok(new ApiResponse(true, "Calificación guardada", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ============ OBTENER MI RATING ACTUAL =============
    @GetMapping("/me")
    public ResponseEntity<?> getMyRating(@PathVariable Long articleId) {
        try {
            User currentUser = securityUtil.getCurrentUser();
            RatingResponse response = ratingService.getMyRating(articleId, currentUser);
            return ResponseEntity.ok(new ApiResponse(true, "Rating obtenido", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ApiResponse(false, e.getMessage(), null));
        }
    }
}