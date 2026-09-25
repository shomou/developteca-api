package com.developteca.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.developteca.dto.CommentCreateRequest;
import com.developteca.dto.CommentResponse;
import com.developteca.entity.Article;
import com.developteca.entity.Comment;
import com.developteca.entity.CommentStatus;
import com.developteca.entity.Role;
import com.developteca.entity.User;
import com.developteca.exception.ApiException;
import com.developteca.repository.ArticleRepository;
import com.developteca.repository.CommentRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ArticleRepository articleRepository;

    @InjectMocks
    private CommentService commentService;

    private Article articulo;
    private User autor;
    private User otroUsuario;
    private User admin;

    @BeforeEach
    void setUp() {
        articulo = new Article();
        articulo.setId(1L);
        articulo.setCommentsCount(0);

        autor = usuario(10L, Role.USER);
        otroUsuario = usuario(11L, Role.USER);
        admin = usuario(99L, Role.ADMIN);
    }

    private User usuario(Long id, Role rol) {
        User u = new User();
        u.setId(id);
        u.setFirstName("Nombre" + id);
        u.setLastName("Apellido" + id);
        u.setRole(rol);
        return u;
    }

    private Comment comentario(Long id, Comment padre, CommentStatus estado, User quien) {
        Comment c = new Comment("texto " + id, articulo, quien, padre);
        c.setId(id);
        c.setStatus(estado);
        return c;
    }

    @Nested
    @DisplayName("crear")
    class Crear {

        @Test
        @DisplayName("nace APPROVED e incrementa el contador del artículo")
        void naceAprobadoYCuenta() {
            CommentCreateRequest req = new CommentCreateRequest();
            req.setContent("Muy buen artículo");

            when(articleRepository.findById(1L)).thenReturn(Optional.of(articulo));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setId(100L);
                return c;
            });

            CommentResponse res = commentService.create(1L, req, autor);

            assertThat(res.getContent()).isEqualTo("Muy buen artículo");
            assertThat(res.getStatus()).isEqualTo(CommentStatus.APPROVED);
            assertThat(articulo.getCommentsCount()).isEqualTo(1);
            verify(articleRepository).save(articulo);
        }

        @Test
        @DisplayName("rechaza responder a un comentario de otro artículo")
        void rechazaPadreDeOtroArticulo() {
            Article otroArticulo = new Article();
            otroArticulo.setId(2L);

            Comment padreAjeno = new Comment("ajeno", otroArticulo, autor, null);
            padreAjeno.setId(50L);

            CommentCreateRequest req = new CommentCreateRequest();
            req.setContent("respuesta");
            req.setParentCommentId(50L);

            when(articleRepository.findById(1L)).thenReturn(Optional.of(articulo));
            when(commentRepository.findById(50L)).thenReturn(Optional.of(padreAjeno));

            assertThatThrownBy(() -> commentService.create(1L, req, autor))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("no pertenece a este");

            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("falla si el artículo no existe")
        void fallaSiNoHayArticulo() {
            when(articleRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.create(404L, new CommentCreateRequest(), autor))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Artículo no encontrado");
        }
    }

    @Nested
    @DisplayName("árbol de comentarios")
    class Arbol {

        @Test
        @DisplayName("anida las respuestas bajo su comentario padre")
        void anidaRespuestas() {
            Comment raiz = comentario(1L, null, CommentStatus.APPROVED, autor);
            Comment respuesta = comentario(2L, raiz, CommentStatus.APPROVED, otroUsuario);
            Comment respuestaDeRespuesta = comentario(3L, respuesta, CommentStatus.APPROVED, autor);
            Comment otraRaiz = comentario(4L, null, CommentStatus.APPROVED, autor);

            when(commentRepository.findByArticleIdAndStatusOrderByCreatedAtAsc(1L, CommentStatus.APPROVED))
                    .thenReturn(List.of(raiz, respuesta, respuestaDeRespuesta, otraRaiz));

            List<CommentResponse> arbol = commentService.getTreeByArticle(1L, false, null);

            // Solo las raíces en el primer nivel
            assertThat(arbol).extracting(CommentResponse::getId).containsExactly(1L, 4L);
            // Anidación de profundidad arbitraria
            assertThat(arbol.get(0).getReplies()).extracting(CommentResponse::getId).containsExactly(2L);
            assertThat(arbol.get(0).getReplies().get(0).getReplies())
                    .extracting(CommentResponse::getId).containsExactly(3L);
            assertThat(arbol.get(1).getReplies()).isEmpty();
        }

        @Test
        @DisplayName("un visitante anónimo solo ve los aprobados")
        void anonimoSoloVeAprobados() {
            when(commentRepository.findByArticleIdAndStatusOrderByCreatedAtAsc(1L, CommentStatus.APPROVED))
                    .thenReturn(List.of());

            commentService.getTreeByArticle(1L, true, null);

            // includeRejected=true se ignora sin usuario: nunca se consulta la lista completa
            verify(commentRepository, never()).findByArticleIdOrderByCreatedAtAsc(any());
        }

        @Test
        @DisplayName("un usuario normal no puede ver los ocultos aunque lo pida")
        void usuarioNormalNoVeOcultos() {
            when(commentRepository.findByArticleIdAndStatusOrderByCreatedAtAsc(1L, CommentStatus.APPROVED))
                    .thenReturn(List.of());

            commentService.getTreeByArticle(1L, true, otroUsuario);

            verify(commentRepository, never()).findByArticleIdOrderByCreatedAtAsc(any());
        }

        @Test
        @DisplayName("un admin que lo pide sí ve los ocultos")
        void adminVeOcultos() {
            Comment oculto = comentario(1L, null, CommentStatus.REJECTED, autor);
            when(commentRepository.findByArticleIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(oculto));

            List<CommentResponse> arbol = commentService.getTreeByArticle(1L, true, admin);

            assertThat(arbol).hasSize(1);
            assertThat(arbol.get(0).getStatus()).isEqualTo(CommentStatus.REJECTED);
        }

        @Test
        @DisplayName("un admin que NO lo pide ve la vista pública")
        void adminSinFlagVeVistaPublica() {
            when(commentRepository.findByArticleIdAndStatusOrderByCreatedAtAsc(1L, CommentStatus.APPROVED))
                    .thenReturn(List.of());

            commentService.getTreeByArticle(1L, false, admin);

            verify(commentRepository, never()).findByArticleIdOrderByCreatedAtAsc(any());
        }
    }

    @Nested
    @DisplayName("moderar")
    class Moderar {

        @Test
        @DisplayName("solo un admin puede moderar")
        void soloAdmin() {
            assertThatThrownBy(() -> commentService.moderate(1L, CommentStatus.REJECTED, otroUsuario))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Solo un administrador");

            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("ocultar un comentario aprobado descuenta del contador")
        void ocultarDescuenta() {
            articulo.setCommentsCount(3);
            Comment c = comentario(1L, null, CommentStatus.APPROVED, autor);
            when(commentRepository.findById(1L)).thenReturn(Optional.of(c));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

            commentService.moderate(1L, CommentStatus.REJECTED, admin);

            assertThat(articulo.getCommentsCount()).isEqualTo(2);
            assertThat(c.getStatus()).isEqualTo(CommentStatus.REJECTED);
        }

        @Test
        @DisplayName("restaurar un comentario oculto vuelve a sumarlo")
        void restaurarSuma() {
            articulo.setCommentsCount(2);
            Comment c = comentario(1L, null, CommentStatus.REJECTED, autor);
            when(commentRepository.findById(1L)).thenReturn(Optional.of(c));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

            commentService.moderate(1L, CommentStatus.APPROVED, admin);

            assertThat(articulo.getCommentsCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("moderar al mismo estado NO altera el contador")
        void mismoEstadoNoCuentaDosVeces() {
            articulo.setCommentsCount(5);
            Comment c = comentario(1L, null, CommentStatus.APPROVED, autor);
            when(commentRepository.findById(1L)).thenReturn(Optional.of(c));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

            commentService.moderate(1L, CommentStatus.APPROVED, admin);

            assertThat(articulo.getCommentsCount()).isEqualTo(5);
            verify(articleRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("eliminar")
    class Eliminar {

        @Test
        @DisplayName("el autor puede borrar su propio comentario")
        void autorBorraElSuyo() {
            articulo.setCommentsCount(1);
            Comment c = comentario(1L, null, CommentStatus.APPROVED, autor);
            when(commentRepository.findById(1L)).thenReturn(Optional.of(c));

            commentService.delete(1L, autor);

            verify(commentRepository).delete(c);
            assertThat(articulo.getCommentsCount()).isZero();
        }

        @Test
        @DisplayName("un admin puede borrar el comentario de otro")
        void adminBorraElDeOtro() {
            Comment c = comentario(1L, null, CommentStatus.APPROVED, autor);
            when(commentRepository.findById(1L)).thenReturn(Optional.of(c));

            commentService.delete(1L, admin);

            verify(commentRepository).delete(c);
        }

        @Test
        @DisplayName("un usuario ajeno NO puede borrarlo")
        void ajenoNoPuedeBorrar() {
            Comment c = comentario(1L, null, CommentStatus.APPROVED, autor);
            when(commentRepository.findById(1L)).thenReturn(Optional.of(c));

            assertThatThrownBy(() -> commentService.delete(1L, otroUsuario))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("No tienes permiso");

            verify(commentRepository, never()).delete(any(Comment.class));
        }

        @Test
        @DisplayName("borrar uno ya oculto no descuenta del contador")
        void ocultoNoDescuenta() {
            articulo.setCommentsCount(4);
            Comment c = comentario(1L, null, CommentStatus.REJECTED, autor);
            when(commentRepository.findById(1L)).thenReturn(Optional.of(c));

            commentService.delete(1L, autor);

            // Ya se había descontado al ocultarlo; volver a hacerlo lo dejaría desfasado.
            assertThat(articulo.getCommentsCount()).isEqualTo(4);
            verify(articleRepository, never()).save(any());
        }

        @Test
        @DisplayName("el contador nunca baja de cero")
        void contadorNoNegativo() {
            articulo.setCommentsCount(0);
            Comment c = comentario(1L, null, CommentStatus.APPROVED, autor);
            when(commentRepository.findById(1L)).thenReturn(Optional.of(c));

            commentService.delete(1L, autor);

            assertThat(articulo.getCommentsCount()).isZero();
        }
    }
}
