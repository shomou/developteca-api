package com.developteca.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.developteca.IntegrationTestBase;
import com.developteca.entity.Article;
import com.developteca.entity.ArticleStatus;
import com.developteca.entity.Category;
import com.developteca.entity.Role;
import com.developteca.entity.User;
import com.developteca.entity.UserStatus;
import com.developteca.repository.ArticleRepository;
import com.developteca.repository.CategoryRepository;
import com.developteca.repository.UserRepository;
import com.developteca.service.JwtService;

/**
 * Cubre los límites de seguridad de la API. Cada bloque corresponde a un fallo real
 * que llegó a estar presente en el proyecto y se detectó probando a mano.
 */
@DisplayName("Límites de seguridad de la API")
class SecurityBoundariesIT extends IntegrationTestBase {

    @Autowired private UserRepository userRepository;
    @Autowired private ArticleRepository articleRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private String tokenAutor;
    private String tokenAdmin;
    private Article borrador;
    private Article publicado;

    @BeforeEach
    void preparar() {
        articleRepository.deleteAll();
        userRepository.deleteAll();

        User autor = crearUsuario("autor@test.local", Role.USER);
        crearUsuario("admin@test.local", Role.ADMIN);

        tokenAutor = jwtService.generateToken("autor@test.local");
        tokenAdmin = jwtService.generateToken("admin@test.local");

        Category categoria = categoryRepository.findBySlug("backend")
                .orElseGet(() -> categoryRepository.save(new Category("Backend", "backend", "d")));

        borrador = crearArticulo("Borrador secreto", "borrador-secreto", autor, categoria, ArticleStatus.DRAFT);
        publicado = crearArticulo("Artículo visible", "articulo-visible", autor, categoria, ArticleStatus.PUBLISHED);
    }

    private User crearUsuario(String email, Role rol) {
        User u = new User(email, passwordEncoder.encode("Password123!"), "Nombre", "Apellido");
        u.setRole(rol);
        u.setStatus(UserStatus.ACTIVE);
        return userRepository.save(u);
    }

    private Article crearArticulo(String titulo, String slug, User autor, Category cat, ArticleStatus estado) {
        Article a = new Article(titulo, slug, "contenido del artículo", autor, cat);
        a.setStatus(estado);
        return articleRepository.save(a);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Nested
    @DisplayName("los borradores no se filtran al público")
    class BorradoresNoSeFiltran {

        // Regresión: getBySlug no filtraba por estado. Como los slugs se derivan del
        // título, cualquiera podía adivinar el de un borrador y leerlo entero sin token.
        @Test
        @DisplayName("GET /articles/{slug} de un borrador responde 404 sin token")
        void borradorPorSlugDa404() throws Exception {
            mockMvc.perform(get("/api/v1/articles/{slug}", borrador.getSlug()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("un borrador sigue oculto incluso para su propio autor por esta vía")
        void borradorOcultoInclusoParaElAutor() throws Exception {
            // El editor usa /manage/{id}; esta ruta pública no debe exponerlo nunca.
            mockMvc.perform(get("/api/v1/articles/{slug}", borrador.getSlug())
                            .header(HttpHeaders.AUTHORIZATION, bearer(tokenAutor)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("el listado público solo incluye PUBLISHED")
        void listadoPublicoSoloPublicados() throws Exception {
            mockMvc.perform(get("/api/v1/articles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].slug").value(publicado.getSlug()));
        }

        @Test
        @DisplayName("un artículo publicado sí es accesible sin token")
        void publicadoEsAccesible() throws Exception {
            mockMvc.perform(get("/api/v1/articles/{slug}", publicado.getSlug()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("Artículo visible"));
        }
    }

    @Nested
    @DisplayName("los endpoints de gestión exigen autenticación")
    class GestionExigeToken {

        // Regresión: /manage cae bajo el matcher público GET /api/v1/articles/**.
        // Sin un matcher específico ANTES, los borradores de todos quedaban expuestos.
        @Test
        @DisplayName("GET /articles/manage sin token da 401")
        void manageSinTokenDa401() throws Exception {
            mockMvc.perform(get("/api/v1/articles/manage"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /articles/manage/{id} sin token da 401")
        void manageIdSinTokenDa401() throws Exception {
            mockMvc.perform(get("/api/v1/articles/manage/{id}", borrador.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /articles/stats/dashboard sin token da 401")
        void statsSinTokenDa401() throws Exception {
            mockMvc.perform(get("/api/v1/articles/stats/dashboard"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("con token, /manage sí muestra los borradores")
        void conTokenMuestraBorradores() throws Exception {
            mockMvc.perform(get("/api/v1/articles/manage")
                            .header(HttpHeaders.AUTHORIZATION, bearer(tokenAutor)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(2));
        }

        @Test
        @DisplayName("un token inválido se rechaza con 401")
        void tokenInvalidoDa401() throws Exception {
            mockMvc.perform(get("/api/v1/articles/manage")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer esto.no.es.un.token"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("un token firmado con otro secreto se rechaza")
        void tokenDeOtroSecretoDa401() throws Exception {
            // Token con estructura válida pero firma ajena: así se vería un intento de
            // suplantación con el jwt.secret que quedó expuesto en el historial.
            String ajeno = "eyJhbGciOiJIUzI1NiJ9."
                    + "eyJzdWIiOiJhZG1pbkB0ZXN0LmxvY2FsIiwiZXhwIjo5OTk5OTk5OTk5fQ."
                    + "firma_que_no_corresponde_a_nuestro_secreto";

            mockMvc.perform(get("/api/v1/articles/manage")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ajeno))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("endpoints públicos que deben seguir abiertos")
    class PublicosSiguenAbiertos {

        @Test
        @DisplayName("GET /categories es público")
        void categoriasEsPublico() throws Exception {
            mockMvc.perform(get("/api/v1/categories")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("/actuator/health es público")
        void healthEsPublico() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }

        @Test
        @DisplayName("/actuator/health NO expone detalles internos")
        void healthSinDetalles() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(jsonPath("$.components").doesNotExist());
        }

        @Test
        @DisplayName("el resto de actuator NO está expuesto")
        void restoDeActuatorCerrado() throws Exception {
            // /actuator/env volcaría todas las variables de entorno, incluido JWT_SECRET.
            int codigo = mockMvc.perform(get("/actuator/env"))
                    .andReturn().getResponse().getStatus();

            assertThat(codigo).isIn(401, 404);
        }
    }

    @Nested
    @DisplayName("CORS está centralizado y restringido")
    class Cors {

        // Regresión: AuthController tenía @CrossOrigin(origins = "*") a nivel de clase,
        // que anulaba la configuración global justo en los endpoints con credenciales.
        @Test
        @DisplayName("un origen permitido recibe la cabecera en /auth/login")
        void origenPermitidoEnLogin() throws Exception {
            mockMvc.perform(options("/api/v1/auth/login")
                            .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
        }

        @Test
        @DisplayName("un origen ajeno NO recibe cabecera CORS en /auth/login")
        void origenAjenoEnLogin() throws Exception {
            mockMvc.perform(options("/api/v1/auth/login")
                            .header(HttpHeaders.ORIGIN, "http://evil.example")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        }

        @Test
        @DisplayName("un origen ajeno tampoco lo recibe en los endpoints de artículos")
        void origenAjenoEnArticulos() throws Exception {
            mockMvc.perform(options("/api/v1/articles")
                            .header(HttpHeaders.ORIGIN, "http://evil.example")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        }
    }

    @Nested
    @DisplayName("autenticación")
    class Autenticacion {

        @Test
        @DisplayName("login correcto devuelve un token utilizable")
        void loginDevuelveTokenUtil() throws Exception {
            String respuesta = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"autor@test.local\",\"password\":\"Password123!\"}"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            String token = respuesta.split("\"token\":\"")[1].split("\"")[0];

            mockMvc.perform(get("/api/v1/articles/manage")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("contraseña incorrecta da 401 y no revela si el email existe")
        void passwordIncorrectaDa401() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"autor@test.local\",\"password\":\"incorrecta\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("la contraseña se guarda hasheada, nunca en claro")
        void passwordHasheada() {
            User u = userRepository.findByEmail("autor@test.local").orElseThrow();

            assertThat(u.getPassword())
                    .isNotEqualTo("Password123!")
                    .startsWith("$2");  // prefijo de bcrypt
        }

        @Test
        @DisplayName("ningún endpoint devuelve el hash de la contraseña")
        void nuncaSeDevuelveElHash() throws Exception {
            String respuesta = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"autor@test.local\",\"password\":\"Password123!\"}"))
                    .andReturn().getResponse().getContentAsString();

            assertThat(respuesta).doesNotContain("password", "$2a$", "$2b$");
        }
    }
}
