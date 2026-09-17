package com.developteca.controller;

import com.developteca.entity.ArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.developteca.dto.ArticleCreateRequest;
import com.developteca.dto.ArticleDetailResponse;
import com.developteca.dto.ArticleImageResponse;
import com.developteca.dto.ArticleSummaryResponse;
import com.developteca.dto.ArticleUpdateRequest;
import com.developteca.dto.DashboardStatsResponse;
import com.developteca.entity.User;
import com.developteca.service.ArticleService;
import com.developteca.util.ApiResponse;
import com.developteca.util.SecurityUtil;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/articles")
public class ArticleController {

    private final ArticleService articleService;
    private final SecurityUtil securityUtil;

    public ArticleController(ArticleService articleService, SecurityUtil securityUtil){
        this.articleService = articleService;  
        this.securityUtil = securityUtil;
    }

    // ============ LISTAR ARTICULOS PUBLICADOS (públicos) =========
    @GetMapping
    public ResponseEntity<?> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String search
    ){
        try{
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            Page<ArticleSummaryResponse> result = articleService.listPuvlished(category, search, pageable);

            return ResponseEntity.ok(new ApiResponse(true, "Articulos obtenidos", result));
        } catch(Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse(false, e.getMessage(), null)
            );
        }
    }

    // ================ VER ARTICULOS POR SLUG (publico) ============
    @GetMapping("/{slug}")
    public ResponseEntity<?> getBySlug(@PathVariable String slug){
        try{
            ArticleDetailResponse article = articleService.getBySlug(slug);
            return ResponseEntity.ok(new ApiResponse(true, "Articulo encontrado", article));
        } catch(Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ============ CREAR ARTICULO (requiere autenticación) ===============
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ArticleCreateRequest request){
        try{
            User currentUser = securityUtil.getCurrentUser();
            ArticleDetailResponse created = articleService.create(request, currentUser);
            
                return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse(true, "Articulo creado exitosamente", created)
            );
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiResponse(false,e.getMessage(),null)
            );
        }        
    }

    // ============= ACTUALIZAR ARTICULO (requiere autenticacion) ===============
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody ArticleUpdateRequest request){
        try{
            User currentUser = securityUtil.getCurrentUser();
            ArticleDetailResponse updated = articleService.update(id, request, currentUser);

            return ResponseEntity.ok(new ApiResponse(true, "Artículo actualizado", updated));
        } catch(Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiResponse(false, e.getMessage(), null)
            );
        }
    }

    // ============= ELIMINAR ARTICULO (requiere autenticacion) ==============
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id){
        try{
            User currentUser = securityUtil.getCurrentUser();
            articleService.delete(id, currentUser);

            return ResponseEntity.noContent().build();
        } catch(Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiResponse(false, e.getMessage(), null));
        }
    }


    // =========== SUBIR IMAGEN A UN ARTICULO (requiere autenticacion) ===========
    @PostMapping("/{id}/images")
    public ResponseEntity<?> addImage(
        @PathVariable Long id,
        @RequestParam("file") MultipartFile file,
        @RequestParam(required = false) String altText,
        @RequestParam(defaultValue = "false") boolean isFeatured,
        @RequestParam(required = false) Integer orderIndex       
    ){
        try{
            User currentUser = securityUtil.getCurrentUser();
            ArticleImageResponse image =  articleService.addImage(id, file, altText, isFeatured, orderIndex, currentUser);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse(true, "Imagen subida exitosamente", image)
            );
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiResponse(false, e.getMessage(), null)
            );
        }
    }

    // ============= ELIMINAR IMAGEN DE UN ARTÍCULO (requiere autenticación) =============
    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<?> deleteImage(@PathVariable Long id, @PathVariable Long imageId) {
        try {
            User currentUser = securityUtil.getCurrentUser();
            articleService.deleteImage(id, imageId, currentUser);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ===========  DASHBOARD STATS (requiere autenticación) ==============
    @GetMapping("/stats/dashboard")
    public ResponseEntity<?> getDashboardStats(){
        try{
            DashboardStatsResponse stats = articleService.getDashboardStats();
            return ResponseEntity.ok(new ApiResponse(true, "Estadísticas obtenidas", stats));
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse(false, e.getMessage(), null)
            );
        }
    }

    // ============ LISTAR MIS ARTICULOS / TODOS SI SOY ADMIN (requiere autenticación) =============
    @GetMapping("/manage")
    public ResponseEntity<?> listForManagement(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) ArticleStatus status
    ) {
        try {
            User currentUser = securityUtil.getCurrentUser();
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            Page<ArticleSummaryResponse> result = articleService.listForManagement(currentUser, status, pageable);

            return ResponseEntity.ok(new ApiResponse(true, "Artículos obtenidos", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ApiResponse(false, e.getMessage(), null));
        }
    }

    // ============ OBTENER ARTICULO PARA EDITAR (requiere autenticación) =============
    @GetMapping("/manage/{id}")
    public ResponseEntity<?> getForEdit(@PathVariable Long id) {
        try {
            User currentUser = securityUtil.getCurrentUser();
            ArticleDetailResponse article = articleService.getForEdit(id, currentUser);
            return ResponseEntity.ok(new ApiResponse(true, "Artículo obtenido", article));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ApiResponse(false, e.getMessage(), null));
        }
    }
}
