package com.developteca.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.developteca.dto.RatingResponse;
import com.developteca.entity.Article;
import com.developteca.entity.Rating;
import com.developteca.entity.User;
import com.developteca.exception.ApiException;
import com.developteca.repository.ArticleRepository;
import com.developteca.repository.RatingRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("RatingService")
class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private ArticleRepository articleRepository;

    @InjectMocks
    private RatingService ratingService;

    private Article articulo;
    private User usuario;

    @BeforeEach
    void setUp() {
        articulo = new Article();
        articulo.setId(1L);
        articulo.setAverageRating(BigDecimal.ZERO);

        usuario = new User();
        usuario.setId(7L);
    }

    @Test
    @DisplayName("la primera calificación crea una fila nueva")
    void primeraCalificacionCrea() {
        when(articleRepository.findById(1L)).thenReturn(Optional.of(articulo));
        when(ratingRepository.findByArticleIdAndUserId(1L, 7L)).thenReturn(Optional.empty());
        when(ratingRepository.averageByArticleId(1L)).thenReturn(5.0);

        RatingResponse res = ratingService.upsert(1L, 5, usuario);

        ArgumentCaptor<Rating> guardado = ArgumentCaptor.forClass(Rating.class);
        verify(ratingRepository).save(guardado.capture());

        assertThat(guardado.getValue().getId()).isNull();     // es nueva
        assertThat(guardado.getValue().getValue()).isEqualTo(5);
        assertThat(res.getMyRating()).isEqualTo(5);
        assertThat(res.getAverageRating()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("volver a calificar ACTUALIZA la fila existente, no crea otra")
    void segundaCalificacionActualiza() {
        Rating existente = new Rating(articulo, usuario, 3);
        existente.setId(42L);

        when(articleRepository.findById(1L)).thenReturn(Optional.of(articulo));
        when(ratingRepository.findByArticleIdAndUserId(1L, 7L)).thenReturn(Optional.of(existente));
        when(ratingRepository.averageByArticleId(1L)).thenReturn(4.0);

        ratingService.upsert(1L, 4, usuario);

        ArgumentCaptor<Rating> guardado = ArgumentCaptor.forClass(Rating.class);
        verify(ratingRepository).save(guardado.capture());

        // Reutiliza la fila: conserva el id, así que save() hace UPDATE y no viola
        // la restricción única (article_id, user_id).
        assertThat(guardado.getValue().getId()).isEqualTo(42L);
        assertThat(guardado.getValue().getValue()).isEqualTo(4);
    }

    @ParameterizedTest(name = "promedio {0} -> se guarda {1}")
    @DisplayName("redondea el promedio a 1 decimal, para encajar en precision=2 scale=1")
    @CsvSource({
            "4.666666, 4.7",
            "4.6499999, 4.6",
            "3.05,      3.1",
            "5.0,       5.0",
            "0.0,       0.0"
    })
    void redondeaPromedio(double crudo, String esperado) {
        when(articleRepository.findById(1L)).thenReturn(Optional.of(articulo));
        when(ratingRepository.findByArticleIdAndUserId(1L, 7L)).thenReturn(Optional.empty());
        when(ratingRepository.averageByArticleId(1L)).thenReturn(crudo);

        ratingService.upsert(1L, 5, usuario);

        assertThat(articulo.getAverageRating()).isEqualByComparingTo(new BigDecimal(esperado));
        verify(articleRepository).save(articulo);
    }

    @Test
    @DisplayName("getMyRating devuelve null si el usuario aún no ha calificado")
    void miRatingNullSiNoHaCalificado() {
        when(articleRepository.findById(1L)).thenReturn(Optional.of(articulo));
        when(ratingRepository.findByArticleIdAndUserId(1L, 7L)).thenReturn(Optional.empty());

        RatingResponse res = ratingService.getMyRating(1L, usuario);

        // El frontend usa este null para decidir si pinta las estrellas vacías.
        assertThat(res.getMyRating()).isNull();
        assertThat(res.getAverageRating()).isZero();
    }

    @Test
    @DisplayName("getMyRating devuelve el valor guardado y no escribe nada")
    void miRatingDevuelveElGuardado() {
        articulo.setAverageRating(new BigDecimal("4.2"));
        Rating mio = new Rating(articulo, usuario, 3);

        when(articleRepository.findById(1L)).thenReturn(Optional.of(articulo));
        when(ratingRepository.findByArticleIdAndUserId(1L, 7L)).thenReturn(Optional.of(mio));

        RatingResponse res = ratingService.getMyRating(1L, usuario);

        assertThat(res.getMyRating()).isEqualTo(3);
        assertThat(res.getAverageRating()).isEqualTo(4.2);
        verify(ratingRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("falla si el artículo no existe")
    void fallaSiNoHayArticulo() {
        when(articleRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ratingService.upsert(404L, 5, usuario))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("no encontrado");
    }
}
