package com.developteca.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.developteca.dto.ArticleCreateRequest;
import com.developteca.dto.ArticleDetailResponse;
import com.developteca.entity.Article;
import com.developteca.entity.ArticleStatus;
import com.developteca.entity.Category;
import com.developteca.entity.Role;
import com.developteca.entity.User;
import com.developteca.exception.ApiException;
import com.developteca.repository.ArticleImageRepository;
import com.developteca.repository.ArticleRepository;
import com.developteca.repository.CategoryRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleService")
class ArticleServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ArticleImageRepository articleImageRepository;

    @Mock
    private ImageService imageService;

    @InjectMocks
    private ArticleService articleService;

    private User autor;
    private User otroUsuario;
    private User admin;
    private Category categoria;

    @BeforeEach
    void setUp() {
        autor = usuario(10L, Role.USER);
        otroUsuario = usuario(11L, Role.USER);
        admin = usuario(99L, Role.ADMIN);

        categoria = new Category("Backend", "backend", "desc");
        categoria.setId(1L);
    }

    private User usuario(Long id, Role rol) {
        User u = new User();
        u.setId(id);
        u.setFirstName("N" + id);
        u.setLastName("A" + id);
        u.setRole(rol);
        return u;
    }

    private Article articulo(Long id, User propietario, ArticleStatus estado) {
        Article a = new Article("Título", "titulo", "contenido", propietario, categoria);
        a.setId(id);
        a.setStatus(estado);
        a.setViewsCount(0);
        a.setCommentsCount(0);
        return a;
    }

    @Nested
    @DisplayName("getBySlug (endpoint público)")
    class GetBySlug {

        // Regresión: getBySlug no filtraba por estado, así que cualquiera que adivinara
        // el slug de un borrador (se derivan del título) podía leerlo entero sin token.
        @Test
        @DisplayName("un BORRADOR responde como no encontrado")
        void borradorNoSeFiltra() {
            Article borrador = articulo(1L, autor, ArticleStatus.DRAFT);
            when(articleRepository.findBySlug("mi-borrador")).thenReturn(Optional.of(borrador));

            assertThatThrownBy(() -> articleService.getBySlug("mi-borrador"))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("no encontrado");

            // No debe ni contarse la visita
            verify(articleRepository, never()).save(any());
        }

        @Test
        @DisplayName("un ARCHIVADO tampoco es accesible")
        void archivadoNoSeFiltra() {
            Article archivado = articulo(1L, autor, ArticleStatus.ARCHIVED);
            when(articleRepository.findBySlug("archivado")).thenReturn(Optional.of(archivado));

            assertThatThrownBy(() -> articleService.getBySlug("archivado"))
                    .isInstanceOf(ApiException.class);
        }

        @Test
        @DisplayName("un PUBLICADO se devuelve e incrementa las vistas")
        void publicadoSeDevuelveYCuenta() {
            Article publicado = articulo(1L, autor, ArticleStatus.PUBLISHED);
            publicado.setViewsCount(41);
            when(articleRepository.findBySlug("publicado")).thenReturn(Optional.of(publicado));

            ArticleDetailResponse res = articleService.getBySlug("publicado");

            assertThat(res.getId()).isEqualTo(1L);
            assertThat(publicado.getViewsCount()).isEqualTo(42);
            verify(articleRepository).save(publicado);
        }

        @Test
        @DisplayName("un slug inexistente da el mismo error que un borrador")
        void inexistenteIndistinguibleDeBorrador() {
            when(articleRepository.findBySlug("nada")).thenReturn(Optional.empty());

            // Mismo mensaje en ambos casos: no revela siquiera que el slug existe.
            assertThatThrownBy(() -> articleService.getBySlug("nada"))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("no encontrado");
        }
    }

    @Nested
    @DisplayName("getForEdit (endpoint de gestión)")
    class GetForEdit {

        @Test
        @DisplayName("el autor puede abrir su propio borrador SIN sumar vistas")
        void autorAbreSuBorrador() {
            Article borrador = articulo(1L, autor, ArticleStatus.DRAFT);
            borrador.setViewsCount(5);
            when(articleRepository.findById(1L)).thenReturn(Optional.of(borrador));

            ArticleDetailResponse res = articleService.getForEdit(1L, autor);

            assertThat(res.getId()).isEqualTo(1L);
            // Abrir el editor no es una lectura del artículo.
            assertThat(borrador.getViewsCount()).isEqualTo(5);
            verify(articleRepository, never()).save(any());
        }

        @Test
        @DisplayName("un admin puede abrir el borrador de otro")
        void adminAbreElDeOtro() {
            Article borrador = articulo(1L, autor, ArticleStatus.DRAFT);
            when(articleRepository.findById(1L)).thenReturn(Optional.of(borrador));

            assertThat(articleService.getForEdit(1L, admin)).isNotNull();
        }

        @Test
        @DisplayName("un usuario ajeno NO puede abrirlo")
        void ajenoNoPuede() {
            Article borrador = articulo(1L, autor, ArticleStatus.DRAFT);
            when(articleRepository.findById(1L)).thenReturn(Optional.of(borrador));

            assertThatThrownBy(() -> articleService.getForEdit(1L, otroUsuario))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("No tienes permiso");
        }
    }

    @Nested
    @DisplayName("crear")
    class Crear {

        private ArticleCreateRequest peticion(String titulo, ArticleStatus estado) {
            ArticleCreateRequest r = new ArticleCreateRequest();
            r.setTitle(titulo);
            r.setContent("contenido");
            r.setCategoryId(1L);
            r.setStatus(estado);
            return r;
        }

        @Test
        @DisplayName("genera el slug a partir del título")
        void generaSlug() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(categoria));
            when(articleRepository.existsBySlug(anyString())).thenReturn(false);
            when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));

            articleService.create(peticion("Hola Mundo en Java", ArticleStatus.DRAFT), autor);

            ArgumentCaptor<Article> guardado = ArgumentCaptor.forClass(Article.class);
            verify(articleRepository).save(guardado.capture());
            assertThat(guardado.getValue().getSlug()).isEqualTo("hola-mundo-en-java");
        }

        @Test
        @DisplayName("añade sufijo numérico si el slug ya existe")
        void slugUnicoConSufijo() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(categoria));
            // El slug base y el -1 ya están tomados; el -2 queda libre.
            when(articleRepository.existsBySlug("hola-mundo")).thenReturn(true);
            when(articleRepository.existsBySlug("hola-mundo-1")).thenReturn(true);
            when(articleRepository.existsBySlug("hola-mundo-2")).thenReturn(false);
            when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));

            articleService.create(peticion("Hola Mundo", ArticleStatus.DRAFT), autor);

            ArgumentCaptor<Article> guardado = ArgumentCaptor.forClass(Article.class);
            verify(articleRepository).save(guardado.capture());
            assertThat(guardado.getValue().getSlug()).isEqualTo("hola-mundo-2");
        }

        @Test
        @DisplayName("sin estado explícito nace como BORRADOR")
        void sinEstadoEsBorrador() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(categoria));
            when(articleRepository.existsBySlug(anyString())).thenReturn(false);
            when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));

            articleService.create(peticion("Sin estado", null), autor);

            ArgumentCaptor<Article> guardado = ArgumentCaptor.forClass(Article.class);
            verify(articleRepository).save(guardado.capture());
            assertThat(guardado.getValue().getStatus()).isEqualTo(ArticleStatus.DRAFT);
            assertThat(guardado.getValue().getPublishedAt()).isNull();
        }

        @Test
        @DisplayName("crear ya PUBLICADO fija la fecha de publicación")
        void publicadoFijaFecha() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(categoria));
            when(articleRepository.existsBySlug(anyString())).thenReturn(false);
            when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));

            articleService.create(peticion("Publicado ya", ArticleStatus.PUBLISHED), autor);

            ArgumentCaptor<Article> guardado = ArgumentCaptor.forClass(Article.class);
            verify(articleRepository).save(guardado.capture());
            assertThat(guardado.getValue().getPublishedAt()).isNotNull();
        }

        @Test
        @DisplayName("falla si la categoría no existe")
        void fallaSinCategoria() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> articleService.create(peticion("X", ArticleStatus.DRAFT), autor))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Categoria no encontrada");
        }
    }

    @Nested
    @DisplayName("eliminar")
    class Eliminar {

        @Test
        @DisplayName("el autor puede borrar el suyo")
        void autorBorra() {
            Article a = articulo(1L, autor, ArticleStatus.PUBLISHED);
            when(articleRepository.findById(1L)).thenReturn(Optional.of(a));

            articleService.delete(1L, autor);

            verify(articleRepository).delete(a);
        }

        @Test
        @DisplayName("un usuario ajeno NO puede borrarlo")
        void ajenoNoBorra() {
            Article a = articulo(1L, autor, ArticleStatus.PUBLISHED);
            when(articleRepository.findById(1L)).thenReturn(Optional.of(a));

            assertThatThrownBy(() -> articleService.delete(1L, otroUsuario))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("No tienes permiso");

            verify(articleRepository, never()).delete(any(Article.class));
        }
    }

    @Nested
    @DisplayName("listForManagement")
    class ListForManagement {

        @Test
        @DisplayName("un admin ve los artículos de todos (authorId null)")
        void adminVeTodos() {
            when(articleRepository.findForManagement(any(), any(), any()))
                    .thenReturn(org.springframework.data.domain.Page.empty());

            articleService.listForManagement(admin, null,
                    org.springframework.data.domain.PageRequest.of(0, 10));

            verify(articleRepository).findForManagement(
                    org.mockito.ArgumentMatchers.isNull(), any(), any());
        }

        @Test
        @DisplayName("un usuario normal solo ve los suyos")
        void usuarioVeLosSuyos() {
            when(articleRepository.findForManagement(any(), any(), any()))
                    .thenReturn(org.springframework.data.domain.Page.empty());

            articleService.listForManagement(autor, null,
                    org.springframework.data.domain.PageRequest.of(0, 10));

            verify(articleRepository).findForManagement(
                    org.mockito.ArgumentMatchers.eq(10L), any(), any());
        }
    }
}
