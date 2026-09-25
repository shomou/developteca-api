package com.developteca;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * Comprueba que el contexto de Spring se levanta por completo.
 *
 * Hereda de IntegrationTestBase para usar el PostgreSQL de Testcontainers: antes
 * dependía de una base local concreta, así que `mvn test` solo funcionaba en la
 * máquina del desarrollador y habría fallado en CI.
 */
@DisplayName("Arranque de la aplicación")
class DeveloptecaApiApplicationTests extends IntegrationTestBase {

	@Autowired
	private ApplicationContext context;

	@Test
	@DisplayName("el contexto carga con todos los beans")
	void contextLoads() {
		assertThat(context).isNotNull();
	}

	@Test
	@DisplayName("los beans críticos están registrados")
	void beansCriticosPresentes() {
		// Varios de estos faltaron en algún momento y tumbaron el arranque
		// (ArticleService sin @Service, EmailService sin JavaMailSender).
		assertThat(context.containsBean("articleService")).isTrue();
		assertThat(context.containsBean("commentService")).isTrue();
		assertThat(context.containsBean("ratingService")).isTrue();
		assertThat(context.containsBean("categoryService")).isTrue();
		assertThat(context.containsBean("imageService")).isTrue();
		assertThat(context.containsBean("emailService")).isTrue();
		assertThat(context.containsBean("jwtService")).isTrue();
	}
}
