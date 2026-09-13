package com.developteca.config;

import com.developteca.entity.Category;
import com.developteca.repository.CategoryRepository;
import com.developteca.util.SlugUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    public DataSeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            return; // Ya hay categorías, no volver a sembrar
        }

        List<Category> categories = List.of(
                new Category("Backend", SlugUtil.toSlug("Backend"), "Desarrollo del lado del servidor"),
                new Category("Frontend", SlugUtil.toSlug("Frontend"), "Desarrollo del lado del cliente"),
                new Category("DevOps", SlugUtil.toSlug("DevOps"), "Infraestructura, CI/CD y despliegue"),
                new Category("Bases de Datos", SlugUtil.toSlug("Bases de Datos"), "SQL, NoSQL y modelado de datos"),
                new Category("Buenas Prácticas", SlugUtil.toSlug("Buenas Prácticas"), "Patrones, clean code y arquitectura")
        );

        categoryRepository.saveAll(categories);
        System.out.println("✅ Categorías iniciales sembradas: " + categories.size());
    }
}