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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
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

/**
 * Comentarios sin cuenta. Cubre lo que CommentServiceTest no puede: se escribió
 * cuando CommentStatus solo tenía dos valores y todo comentario tenía autor.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Comentarios anónimos")
class AnonymousCommentTest {

    @Mock private CommentRepository commentRepository;
    @Mock private ArticleRepository articleRepository;
    @InjectMocks private CommentService commentService;

    private Article articulo;
    private User registrado;
    private User admin;

    @BeforeEach
    void setUp() {
        articulo = new Article();
        articulo.setId(1L);
        articulo.setCommentsCount(0);

        registrado = usuario(10L, Role.USER);
        admin = usuario(99L, Role.ADMIN);
    }

    private User usuario(Long id, Role rol) {
        User u = new User();
        u.setId(id);
        u.setFirstName("N" + id);
        u.setLastName("A" + id);
        u.setRole(rol);
        return u;
    }

    private CommentCreateRequest peticion(String contenido, String nombre, String email, String honeypot) {
        CommentCreateRequest r = new CommentCreateRequest();
        r.setContent(contenido);
        r.setAuthorName(nombre);
        r.setAuthorEmail(email);
        r.setWebsite(honeypot);
        return r;
    }

    private void devuelveElComentarioGuardado() {
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(100L);
            return c;
        });
    }

    @Nested
    @DisplayName("estado inicial según quién comenta")
    class EstadoInicial {

        @Test
        @DisplayName("un anónimo queda PENDIENTE de revisión")
        void anonimoQuedaPendiente() {
            when(articleRepository.findById(1L)).thenReturn(Optional.of(articulo));
            devuelveElComentarioGuardado();

            CommentResponse res = commentService.create(
                    1L, peticion("Buen artículo", "María", null, null), null);

            assertThat(res.getStatus()).isEqualTo(CommentStatus.PENDING);
            assertThat(res.getAuthorName()).isEqualTo("María");
            assertThat(res.getAuthor()).isNull();
        }

        @Test
        @DisplayName("un registrado se publica al instante")
        void registradoSePublica() {
            when(articleRepository.findById(1L)).thenReturn(Optional.of(articulo));
            devuelveElComentarioGuardado();

            CommentResponse res = commentService.create(
                    1L, peticion("Buen artículo", null, null, null), registrado);

            assertThat(res.getStatus()).isEqualTo(CommentStatus.APPROVED);
            assertThat(res.getAuthor()).isNotNull();
            assertThat(res.getAuthorName()).isNull();
        }

        @Test
        @DisplayName("el contador solo sube con los que se publican")
        void contadorSoloConPublicados() {
            when(articleRepository.findById(1L)).thenReturn(Optional.of(articulo));
            devuelveElComentarioGuardado();

            commentService.create(1L, peticion("De invitado", "María", null, null), null);
            assertThat(articulo.getCommentsCount())
                    .as("un PENDIENTE no es visible, no debe contarse")
                    .isZero();

            commentService.create(1L, peticion("De usuario", null, null, null), registrado);
            assertThat(articulo.getCommentsCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("el email del anónimo se guarda pero NO se devuelve")
        void emailNoSeExpone() {
            when(articleRepository.findById(1L)).thenReturn(Optional.of(articulo));
            devuelveElComentarioGuardado();

            CommentResponse res = commentService.create(
                    1L, peticion("Hola", "María", "maria@example.com", null), null);

            ArgumentCaptor<Comment> guardado = ArgumentCaptor.forClass(Comment.class);
            verify(commentRepository).save(guardado.capture());

            assertThat(guardado.getValue().getAuthorEmail()).isEqualTo("maria@example.com");
            // CommentResponse no tiene campo de email: no hay forma de filtrarlo.
            assertThat(res.getAuthorName()).isEqualTo("María");
        }

        @Test
        @DisplayName("el email es opcional")
        void emailOpcional() {
            when(articleRepository.findById(1L)).thenReturn(Optional.of(articulo));
            devuelveElComentarioGuardado();

            assertThat(commentService.create(1L, peticion("Hola", "María", null, null), null))
                    .isNotNull();
        }

        @Test
        @DisplayName("se recortan los espacios del nombre")
        void nombreSinEspacios() {
            when(articleRepository.findById(1L)).thenReturn(Optional.of(articulo));
            devuelveElComentarioGuardado();

            CommentResponse res = commentService.create(
                    1L, peticion("Hola", "  María  ", null, null), null);

            assertThat(res.getAuthorName()).isEqualTo("María");
        }
    }

    @Nested
    @DisplayName("validación del nombre")
    class ValidacionNombre {

        @ParameterizedTest(name = "nombre = [{0}]")
        @DisplayName("un anónimo sin nombre utilizable se rechaza")
        @CsvSource(value = {"NULL", "''", "'   '"}, nullValues = "NULL")
        void anonimoSinNombre(String nombre) {
            assertThatThrownBy(() ->
                    commentService.create(1L, peticion("Hola", nombre, null, null), null))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("nombre es obligatorio");

            // Falla antes de tocar la base: no se busca siquiera el artículo.
            verify(articleRepository, never()).findById(any());
            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("un registrado no necesita enviar nombre")
        void registradoNoNecesitaNombre() {
            when(articleRepository.findById(1L)).thenReturn(Optional.of(articulo));
            devuelveElComentarioGuardado();

            assertThat(commentService.create(1L, peticion("Hola", null, null, null), registrado))
                    .isNotNull();
        }
    }

    @Nested
    @DisplayName("trampa anti-spam (honeypot)")
    class Honeypot {

        @Test
        @DisplayName("si el campo oculto viene relleno, se rechaza")
        void campoRellenoSeRechaza() {
            assertThatThrownBy(() -> commentService.create(
                    1L, peticion("SPAM", "Bot", null, "http://spam.example"), null))
                    .isInstanceOf(ApiException.class);

            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("el mensaje de error no revela cuál fue el campo delator")
        void mensajeNoRevelaLaTrampa() {
            assertThatThrownBy(() -> commentService.create(
                    1L, peticion("SPAM", "Bot", null, "x"), null))
                    .hasMessageNotContainingAny("website", "honeypot", "oculto", "trampa");
        }

        @Test
        @DisplayName("también se aplica a usuarios registrados")
        void tambienParaRegistrados() {
            assertThatThrownBy(() -> commentService.create(
                    1L, peticion("SPAM", null, null, "x"), registrado))
                    .isInstanceOf(ApiException.class);
        }

        @ParameterizedTest(name = "website = [{0}]")
        @DisplayName("un campo vacío o en blanco no bloquea a nadie")
        @CsvSource(value = {"NULL", "''", "'   '"}, nullValues = "NULL")
        void vacioNoBloquea(String valor) {
            when(articleRepository.findById(1L)).thenReturn(Optional.of(articulo));
            devuelveElComentarioGuardado();

            assertThat(commentService.create(1L, peticion("Hola", "María", null, valor), null))
                    .isNotNull();
        }
    }

    @Nested
    @DisplayName("visibilidad de los pendientes")
    class VisibilidadPendientes {

        @Test
        @DisplayName("la vista pública solo pide los APPROVED")
        void vistaPublicaSoloAprobados() {
            when(commentRepository.findByArticleIdAndStatusOrderByCreatedAtAsc(1L, CommentStatus.APPROVED))
                    .thenReturn(List.of());

            commentService.getTreeByArticle(1L, false, null);

            // Si alguna vez se consultara la lista completa aquí, se publicaría
            // spam sin revisar: este es el punto exacto donde eso ocurriría.
            verify(commentRepository, never()).findByArticleIdOrderByCreatedAtAsc(any());
        }

        @Test
        @DisplayName("un anónimo no puede ver los pendientes ni pidiéndolo")
        void anonimoNoVePendientes() {
            when(commentRepository.findByArticleIdAndStatusOrderByCreatedAtAsc(1L, CommentStatus.APPROVED))
                    .thenReturn(List.of());

            commentService.getTreeByArticle(1L, true, null);

            verify(commentRepository, never()).findByArticleIdOrderByCreatedAtAsc(any());
        }

        @Test
        @DisplayName("un admin sí los ve cuando los pide")
        void adminVePendientes() {
            Comment pendiente = new Comment("de invitado", articulo, "María", null, null);
            pendiente.setId(1L);
            pendiente.setStatus(CommentStatus.PENDING);

            when(commentRepository.findByArticleIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(pendiente));

            List<CommentResponse> arbol = commentService.getTreeByArticle(1L, true, admin);

            assertThat(arbol).hasSize(1);
            assertThat(arbol.get(0).getStatus()).isEqualTo(CommentStatus.PENDING);
            assertThat(arbol.get(0).getAuthorName()).isEqualTo("María");
        }
    }

    @Nested
    @DisplayName("el contador ante las 6 transiciones de estado")
    class ContadorEnTransiciones {

        // Regresión: adjustCommentsCount enumeraba pares de estados y solo cubría
        // APPROVED<->REJECTED. Al añadir PENDING, aprobar un comentario anónimo
        // (PENDING -> APPROVED) no incrementaba el contador, en silencio.
        @ParameterizedTest(name = "{0} -> {1} deja el contador en {3} (partiendo de {2})")
        @DisplayName("solo cuenta lo que está APROBADO")
        @CsvSource({
                "PENDING,  APPROVED, 0, 1",
                "PENDING,  REJECTED, 0, 0",
                "APPROVED, REJECTED, 1, 0",
                "APPROVED, PENDING,  1, 0",
                "REJECTED, APPROVED, 0, 1",
                "REJECTED, PENDING,  0, 0",
                "APPROVED, APPROVED, 1, 1",
                "PENDING,  PENDING,  0, 0"
        })
        void transiciones(CommentStatus desde, CommentStatus hasta, int inicial, int esperado) {
            articulo.setCommentsCount(inicial);

            Comment c = new Comment("texto", articulo, "María", null, null);
            c.setId(1L);
            c.setStatus(desde);

            when(commentRepository.findById(1L)).thenReturn(Optional.of(c));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

            commentService.moderate(1L, hasta, admin);

            assertThat(articulo.getCommentsCount()).isEqualTo(esperado);
            assertThat(c.getStatus()).isEqualTo(hasta);
        }

        @Test
        @DisplayName("el contador nunca baja de cero aunque se desincronice")
        void nuncaNegativo() {
            articulo.setCommentsCount(0);

            Comment c = new Comment("texto", articulo, "María", null, null);
            c.setId(1L);
            c.setStatus(CommentStatus.APPROVED);

            when(commentRepository.findById(1L)).thenReturn(Optional.of(c));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

            commentService.moderate(1L, CommentStatus.REJECTED, admin);

            assertThat(articulo.getCommentsCount()).isZero();
        }
    }

    @Nested
    @DisplayName("permisos sobre comentarios anónimos")
    class PermisosAnonimos {

        @Test
        @DisplayName("un admin puede borrar uno anónimo")
        void adminBorraAnonimo() {
            Comment c = new Comment("texto", articulo, "María", null, null);
            c.setId(1L);
            c.setStatus(CommentStatus.APPROVED);
            articulo.setCommentsCount(1);

            when(commentRepository.findById(1L)).thenReturn(Optional.of(c));

            commentService.delete(1L, admin);

            verify(commentRepository).delete(c);
        }

        @Test
        @DisplayName("un usuario normal NO puede borrar uno anónimo")
        void usuarioNoBorraAnonimo() {
            Comment c = new Comment("texto", articulo, "María", null, null);
            c.setId(1L);
            c.setStatus(CommentStatus.APPROVED);

            when(commentRepository.findById(1L)).thenReturn(Optional.of(c));

            // Sin autor registrado, nadie puede reclamar su propiedad.
            assertThatThrownBy(() -> commentService.delete(1L, registrado))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("No tienes permiso");
        }

        @Test
        @DisplayName("moderar sigue siendo exclusivo de un admin")
        void moderarSoloAdmin() {
            assertThatThrownBy(() ->
                    commentService.moderate(1L, CommentStatus.APPROVED, registrado))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Solo un administrador");

            verify(commentRepository, never()).save(any());
        }
    }
}
