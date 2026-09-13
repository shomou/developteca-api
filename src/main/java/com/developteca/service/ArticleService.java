package com.developteca.service;


import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.developteca.dto.ArticleCreateRequest;
import com.developteca.dto.ArticleDetailResponse;
import com.developteca.dto.ArticleImageResponse;
import com.developteca.dto.ArticleSummaryResponse;
import com.developteca.dto.ArticleUpdateRequest;
import com.developteca.dto.AuthorResponse;
import com.developteca.dto.CategoryResponse;
import com.developteca.dto.DashboardStatsResponse;
import com.developteca.entity.Article;
import com.developteca.entity.ArticleImage;
import com.developteca.entity.ArticleStatus;
import com.developteca.entity.Category;
import com.developteca.entity.Role;
import com.developteca.entity.User;
import com.developteca.exception.ApiException;
import com.developteca.repository.ArticleImageRepository;
import com.developteca.repository.ArticleRepository;
import com.developteca.repository.CategoryRepository;
import com.developteca.util.SlugUtil;

import jakarta.transaction.Transactional;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final ArticleImageRepository articleImageRepository;
    private final ImageService imageService;

    public ArticleService(ArticleRepository articleRepository, CategoryRepository categoryRepository,
                           ArticleImageRepository articleImageRepository, ImageService imageService) {
        this.articleRepository = articleRepository;
        this.categoryRepository = categoryRepository;
        this.articleImageRepository = articleImageRepository;
        this.imageService = imageService;
    }

    // ============ CREAR ARTICULO =============
    @Transactional
    public ArticleDetailResponse create(ArticleCreateRequest request, User author){
        Category category = categoryRepository.findById(request.getCategoryId())
        .orElseThrow(() -> new ApiException("Categoria no encontrada"));

        String baseSlug =  SlugUtil.toSlug(request.getTitle());
        String slug = ensureUniqueSlug(baseSlug);

        Article article = new Article(request.getTitle(), slug, request.getContent(), author, category);
        article.setStatus(request.getStatus() !=null ? request.getStatus() : ArticleStatus.DRAFT);

        if(article.getStatus() == ArticleStatus.PUBLISHED){
            article.setPublishedAt(LocalDateTime.now());
        }

        Article saved = articleRepository.save(article);
        return mapToDetailResponse(saved);
    }

     // ============= ACTUALIZAR ARTÍCULO =============
    @Transactional
    public ArticleDetailResponse update(Long articleId, ArticleUpdateRequest request, User currentUser) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ApiException("Artículo no encontrado"));

        checkOwnershipOrAdmin(article, currentUser);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ApiException("Categoría no encontrada"));

        // Solo regenerar slug si el título cambió
        if (!article.getTitle().equals(request.getTitle())) {
            String baseSlug = SlugUtil.toSlug(request.getTitle());
            article.setSlug(ensureUniqueSlug(baseSlug));
        }

        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        article.setCategory(category);

        // Si pasa de no-publicado a PUBLISHED, marcar publishedAt
        if (article.getStatus() != ArticleStatus.PUBLISHED && request.getStatus() == ArticleStatus.PUBLISHED) {
            article.setPublishedAt(LocalDateTime.now());
        }
        article.setStatus(request.getStatus());

        Article saved = articleRepository.save(article);
        return mapToDetailResponse(saved);
    }

    // ============== ELIMINAR ARTICULO ===========
    public void delete(Long articleId, User currentUser){
        Article article = articleRepository.findById(articleId)
                            .orElseThrow( () -> new ApiException("Articulo no Encontrado"));
        
        checkOwnershipOrAdmin(article, currentUser);

        // Eliminar archivos fisicos de imagenes antes de borrar el articulo
        article.getImages().forEach(img -> {
            if(img.getImageKey() != null){
                imageService.delete(img.getImageKey());
            }
        });

        articleRepository.delete(article);
    }

    //============= OBTENER ARTICULO POR SLUG (público, incrementa vistas) =================
    @Transactional
    public ArticleDetailResponse getBySlug(String slug){
        Article article = articleRepository.findBySlug(slug)
                .orElseThrow(() -> new ApiException("Articulo no encontrado"));
        
            article.setViewsCount(article.getViewsCount() + 1);
            articleRepository.save(article);

            return mapToDetailResponse(article);
    } 
    
    // ============ LISTAR ARTICULOS PUBLICADOS (público, paginado) ====================
    public Page<ArticleSummaryResponse> listPuvlished(String categorySlug, String search, Pageable pageable){
        Page<Article> articles = articleRepository.findByPublishedArticles(
            ArticleStatus.PUBLISHED, categorySlug, search, pageable);

        return articles.map(this::mapToSummaryResponse);
    }

    // ============= AGREGAR IMAGEN A UN ARTICULO ================
    @Transactional
    public ArticleImageResponse addImage(Long articleId, MultipartFile file, String altText,
                                          boolean isFeatured, Integer orderIndex, User currentUser){
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ApiException("Articulo no encontrado"));

        checkOwnershipOrAdmin(article, currentUser);

        ImageService.StoredImage stored = imageService.store(file, articleId);

        // Si esta imagen es featured, desmarcar la anterior festured (solo una por articulo)
        if(isFeatured){
            articleImageRepository.findByArticleIdAndIsFeaturedTrue(articleId)
                    .ifPresent(prev -> {
                        prev.setIsFeatured(false);
                        articleImageRepository.save(prev);
                    });
        }

        ArticleImage image =  new ArticleImage(article, stored.getUrl(), altText, isFeatured,
           orderIndex != null ? orderIndex:0 );
        
        image.setImageKey(stored.getStoragePath());
        image.setFileSize(stored.getFileSize());
        image.setMimeType(stored.getMimeType());

        ArticleImage saved = articleImageRepository.save(image);

        return new ArticleImageResponse(saved.getId(), saved.getImageUrl(), saved.getAltText(),
                saved.getIsFeatured(), saved.getOrderIndex());

    }

    // ============= ELIMINAR IMAGEN =============
    @Transactional
    public void deleteImage(Long articleId, Long imageId, User currentUser) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ApiException("Artículo no encontrado"));

        checkOwnershipOrAdmin(article, currentUser);

        ArticleImage image = articleImageRepository.findById(imageId)
                .orElseThrow(() -> new ApiException("Imagen no encontrada"));

        if (!image.getArticle().getId().equals(articleId)) {
            throw new ApiException("La imagen no pertenece a este artículo");
        }

        if (image.getImageKey() != null) {
            imageService.delete(image.getImageKey());
        }

        articleImageRepository.delete(image);
    }

    // ============ ESTADISTICAS PARA DASHBOARD (solo admin) ===========
    public DashboardStatsResponse getDashboardStats(){
        long total = articleRepository.count();
        long published = articleRepository.countByStatus(ArticleStatus.PUBLISHED);
        long draft = articleRepository.countByStatus(ArticleStatus.DRAFT);
        long archived = articleRepository.countByStatus(ArticleStatus.ARCHIVED);
        long views = articleRepository.sumTotalViews();
        long comments = articleRepository.sumTotalComments();
        double avgRating = articleRepository.averageRatingOverall();

        return new DashboardStatsResponse(total, published, draft, archived, views, comments, avgRating);
    }


    // ============= HELPERS PRIVADOS =============

    private void checkOwnershipOrAdmin(Article article, User currentUser) {
        boolean isOwner = article.getAuthor().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.SUPER_ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ApiException("No tienes permiso para modificar este artículo");
        }
    }

    private String ensureUniqueSlug(String baseSlug) {
        String slug = baseSlug;
        int counter = 1;
        while (articleRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }
        return slug;
    }

    private ArticleDetailResponse mapToDetailResponse(Article article) {
        List<ArticleImageResponse> images = article.getImages().stream()
                .map(img -> new ArticleImageResponse(img.getId(), img.getImageUrl(), img.getAltText(),
                        img.getIsFeatured(), img.getOrderIndex()))
                .collect(Collectors.toList());

        return new ArticleDetailResponse(
                article.getId(), article.getTitle(), article.getSlug(), article.getContent(),
                mapAuthor(article.getAuthor()), mapCategory(article.getCategory()), images,
                article.getAverageRating(), article.getCommentsCount(), article.getViewsCount(),
                article.getStatus(), article.getCreatedAt(), article.getPublishedAt()
        );
    }

    private ArticleSummaryResponse mapToSummaryResponse(Article article) {
        ArticleImageResponse featured = article.getImages().stream()
                .filter(ArticleImage::getIsFeatured)
                .findFirst()
                .map(img -> new ArticleImageResponse(img.getId(), img.getImageUrl(), img.getAltText(),
                        img.getIsFeatured(), img.getOrderIndex()))
                .orElse(null);

        String excerpt = article.getContent().length() > 200
                ? article.getContent().substring(0, 200) + "..."
                : article.getContent();

        return new ArticleSummaryResponse(
                article.getId(), article.getTitle(), article.getSlug(), excerpt,
                mapAuthor(article.getAuthor()), mapCategory(article.getCategory()), featured,
                article.getAverageRating(), article.getCommentsCount(), article.getViewsCount(),
                article.getStatus(), article.getPublishedAt()
        );
    }

    private AuthorResponse mapAuthor(User user) {
        return new AuthorResponse(user.getId(), user.getFirstName(), user.getLastName());
    }

    private CategoryResponse mapCategory(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug(), category.getDescription());
    }



}
