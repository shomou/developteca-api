package com.developteca;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base de los tests de integración.
 *
 * Levanta un PostgreSQL real en Docker en vez de usar H2: la base de código depende
 * de comportamientos propios de PostgreSQL (el CAST de parámetros nulos en las
 * consultas JPQL, las restricciones únicas compuestas), y H2 los simula mal o no los
 * reproduce, con lo que los tests pasarían y producción fallaría.
 *
 * El contenedor es estático para compartirse entre todas las clases de test: arrancar
 * uno por clase multiplicaría el tiempo de ejecución sin aportar aislamiento real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public abstract class IntegrationTestBase {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;
}
