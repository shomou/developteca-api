package com.developteca.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MarkdownUtil.toPlainText")
class MarkdownUtilTest {

    @Test
    @DisplayName("devuelve cadena vacía para null y para texto en blanco")
    void vacioParaNullYBlanco() {
        assertThat(MarkdownUtil.toPlainText(null)).isEmpty();
        assertThat(MarkdownUtil.toPlainText("")).isEmpty();
        assertThat(MarkdownUtil.toPlainText("   \n  ")).isEmpty();
    }

    @Test
    @DisplayName("elimina las marcas de encabezado")
    void quitaEncabezados() {
        assertThat(MarkdownUtil.toPlainText("## Configurando Spring"))
                .isEqualTo("Configurando Spring");
        assertThat(MarkdownUtil.toPlainText("###### Nivel seis"))
                .isEqualTo("Nivel seis");
    }

    @Test
    @DisplayName("conserva el texto pero quita el énfasis")
    void quitaEnfasis() {
        assertThat(MarkdownUtil.toPlainText("esto es **importante** y esto *distinto*"))
                .isEqualTo("esto es importante y esto distinto");
        assertThat(MarkdownUtil.toPlainText("__negrita__ y _cursiva_"))
                .isEqualTo("negrita y cursiva");
    }

    @Test
    @DisplayName("elimina los bloques de código por completo, no solo los backticks")
    void eliminaBloquesDeCodigo() {
        String md = """
                Primero el bean:

                ```java
                @Bean
                public PasswordEncoder passwordEncoder() {
                    return new BCryptPasswordEncoder();
                }
                ```

                Y listo.""";

        // El código nunca es un buen resumen para la tarjeta de un artículo.
        assertThat(MarkdownUtil.toPlainText(md))
                .isEqualTo("Primero el bean: Y listo.")
                .doesNotContain("@Bean", "```", "BCryptPasswordEncoder");
    }

    @Test
    @DisplayName("conserva el contenido del código en línea, sin los backticks")
    void conservaCodigoEnLinea() {
        assertThat(MarkdownUtil.toPlainText("usa el método `main` para arrancar"))
                .isEqualTo("usa el método main para arrancar");
    }

    @Test
    @DisplayName("conserva el texto de los enlaces y descarta la URL")
    void conservaTextoDeEnlaces() {
        assertThat(MarkdownUtil.toPlainText("mira la [documentación oficial](https://spring.io/docs)"))
                .isEqualTo("mira la documentación oficial");
    }

    @Test
    @DisplayName("elimina las imágenes enteras, incluido su texto alternativo")
    void eliminaImagenes() {
        assertThat(MarkdownUtil.toPlainText("antes ![diagrama](/uploads/a.png) después"))
                .isEqualTo("antes después");
    }

    @Test
    @DisplayName("quita las viñetas y la numeración de las listas")
    void quitaMarcasDeLista() {
        String md = """
                - primero
                - segundo
                1. numerado
                2. otro""";

        assertThat(MarkdownUtil.toPlainText(md)).isEqualTo("primero segundo numerado otro");
    }

    @Test
    @DisplayName("quita la marca de cita")
    void quitaCitas() {
        assertThat(MarkdownUtil.toPlainText("> Nunca guardes contraseñas en texto plano."))
                .isEqualTo("Nunca guardes contraseñas en texto plano.");
    }

    @Test
    @DisplayName("colapsa saltos de línea y espacios múltiples en uno solo")
    void colapsaEspacios() {
        assertThat(MarkdownUtil.toPlainText("línea uno\n\n\nlínea    dos"))
                .isEqualTo("línea uno línea dos");
    }

    @Test
    @DisplayName("un artículo completo produce un extracto legible, sin sintaxis")
    void articuloCompletoQuedaLegible() {
        String md = """
                ## Hola Mundo en Java

                Todo lenguaje empieza igual: un **saludo**.

                ```java
                System.out.println("¡Hola!");
                ```

                > El archivo debe llamarse `HolaMundo.java`.

                - Java distingue mayúsculas
                - Cada instrucción lleva punto y coma""";

        String plano = MarkdownUtil.toPlainText(md);

        assertThat(plano)
                .startsWith("Hola Mundo en Java")
                .doesNotContain("#", "```", "**", ">", "System.out");
    }
}
