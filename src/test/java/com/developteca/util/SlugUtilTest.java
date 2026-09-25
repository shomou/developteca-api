package com.developteca.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("SlugUtil")
class SlugUtilTest {

    @Nested
    @DisplayName("regresión: patrón WHITESPACE invertido")
    class RegresionPatronInvertido {

        // El patrón WHITESPACE fue una vez [^\s]+ (runs de NO-espacio) en lugar de \s+.
        // Con eso, "Backend" colapsaba a "-" y el trim final lo dejaba en "", de modo que
        // TODOS los slugs salían vacíos y DataSeeder reventaba el arranque contra la
        // restricción única de slug al insertar la segunda categoría.
        @Test
        @DisplayName("una sola palabra no queda vacía")
        void unaPalabraNoQuedaVacia() {
            assertThat(SlugUtil.toSlug("Backend")).isEqualTo("backend");
        }

        @Test
        @DisplayName("las 5 categorías sembradas generan slugs distintos y no vacíos")
        void categoriasSembradasNoColisionan() {
            String[] nombres = { "Backend", "Frontend", "DevOps", "Bases de Datos", "Buenas Prácticas" };

            assertThat(nombres)
                    .extracting(SlugUtil::toSlug)
                    .doesNotContain("")
                    .doesNotHaveDuplicates();
        }
    }

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @DisplayName("normaliza texto a slug")
    @CsvSource({
            "'Mi primer artículo',            mi-primer-articulo",
            "'Hola Mundo en Java',            hola-mundo-en-java",
            "'MAYÚSCULAS',                    mayusculas",
            "'  espacios  alrededor  ',       espacios-alrededor",
            "'múltiples   espacios',          multiples-espacios",
            "'acentos áéíóú y ñ',             acentos-aeiou-y-n",
            "'signos: ¿qué? ¡sí!',            signos-que-si",
            "'guiones--ya--existentes',       guiones-ya-existentes",
            "'123 números 456',               123-numeros-456"
    })
    void normaliza(String entrada, String esperado) {
        assertThat(SlugUtil.toSlug(entrada)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("nunca deja guiones al principio ni al final")
    void sinGuionesEnLosExtremos() {
        assertThat(SlugUtil.toSlug("-!- raro -!-"))
                .doesNotStartWith("-")
                .doesNotEndWith("-");
    }

    @Test
    @DisplayName("nunca produce guiones consecutivos")
    void sinGuionesConsecutivos() {
        assertThat(SlugUtil.toSlug("a  ---  b")).doesNotContain("--");
    }

    @Test
    @DisplayName("es determinista: la misma entrada da el mismo slug")
    void esDeterminista() {
        String titulo = "Despliegue con Docker Compose";
        assertThat(SlugUtil.toSlug(titulo)).isEqualTo(SlugUtil.toSlug(titulo));
    }
}
