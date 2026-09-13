# developteca-api

API REST en Spring Boot 4.1 para **Developteca**, con autenticación JWT y gestión de artículos técnicos (creación, categorías, imágenes) sobre PostgreSQL.

## Stack

- **Java 17** / **Maven**
- **Spring Boot 4.1** — `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `spring-boot-starter-mail`
- **PostgreSQL** (driver `postgresql`, JDBC)
- **JJWT 0.12** (`io.jsonwebtoken`) para firmar/validar tokens
- **Jackson 3** (`tools.jackson.*`) para serialización — Spring Boot 4 ya no usa Jackson 2/`com.fasterxml.jackson.*` para el `ObjectMapper` gestionado por Spring
- **Lombok** (opcional)

## Requisitos previos

- JDK 17
- Maven 3.9+
- PostgreSQL corriendo localmente, con una base de datos `developteca_db`

## Puesta en marcha

1. Crea la base de datos:

   ```sql
   CREATE DATABASE developteca_db;
   ```

2. Ajusta credenciales en `src/main/resources/application.yml` si no usas los valores por defecto (`postgres` / `1234567`, `localhost:5432`).

3. Compila y corre los tests:

   ```bash
   mvn clean install
   ```

4. Levanta la aplicación:

   ```bash
   mvn spring-boot:run
   ```

   La API queda disponible en `http://localhost:8080`.

Al primer arranque, `DataSeeder` siembra 5 categorías por defecto (Backend, Frontend, DevOps, Bases de Datos, Buenas Prácticas) si la tabla `category` está vacía.

## Comandos útiles

```bash
mvn compile                                   # Chequeo rápido de compilación
mvn test                                      # Correr todos los tests
mvn test -Dtest=ClassNameTest                 # Correr una clase de test
mvn test -Dtest=ClassNameTest#methodName       # Correr un método de test
```

## Configuración

Toda la configuración vive en `src/main/resources/application.yml`:

| Bloque | Propósito |
|---|---|
| `spring.datasource` | Conexión a PostgreSQL |
| `spring.jpa` | Hibernate (`ddl-auto: update`, dialecto PostgreSQL) |
| `spring.mail` | SMTP para el envío de emails de verificación — **actualmente apunta a un placeholder local (`localhost:1025`) que no entrega correos reales**; reemplázalo con credenciales SMTP reales antes de depender del envío de emails |
| `jwt` | `secret` y `expiration` (ms) para firmar los JWT — debe ir en el nivel raíz, **no** anidado bajo `spring` |
| `app.upload` | Directorio de subida (`dir`), tamaño máximo (`max-file-size`) y tipos permitidos (`allowed-types`) para imágenes de artículos |
| `server.port` | Puerto HTTP (`8080` por defecto) |

> ⚠️ No existe aún configuración `spring.servlet.multipart.*`, así que aplican los límites por defecto de Spring Boot (1MB por archivo / 10MB por request), **menores** que `app.upload.max-file-size` (5MB) — subidas grandes pueden ser rechazadas antes de llegar a la validación propia de la app.

## Endpoints

Todas las respuestas se envuelven en un sobre `ApiResponse` (`success`, `message`, `data`, `timestamp`, `errors`).

### Auth — `/api/v1/auth`

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| POST | `/register` | pública | Registra un usuario, envía email de verificación |
| POST | `/login` | pública | Devuelve tokens JWT de acceso/refresco |
| POST | `/verify-email?token=...` | pública | Verifica el email con el token enviado |

### Articles — `/api/v1/articles`

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| GET | `/` | pública | Listado paginado (`page`, `size`, `category`, `search`) de artículos publicados |
| GET | `/{slug}` | pública | Detalle de un artículo por slug (incrementa `viewsCount`) |
| POST | `/` | requerida | Crea un artículo (`status` por defecto `DRAFT`) |
| PUT | `/{id}` | requerida (autor o admin) | Actualiza un artículo |
| DELETE | `/{id}` | requerida (autor o admin) | Elimina un artículo y sus imágenes |
| POST | `/{id}/images` | requerida (autor o admin) | Sube una imagen (`multipart/form-data`: `file`, `altText`, `isFeatured`, `orderIndex`) |
| DELETE | `/{id}/images/{imageId}` | requerida (autor o admin) | Elimina una imagen del artículo |

Las imágenes subidas se sirven de forma estática en `/uploads/**`.

## Notas de arquitectura

Para detalles internos (flujo de auth, particularidades de Jackson 3, gotchas de JPQL con PostgreSQL, estructura de entidades, etc.), ver [CLAUDE.md](CLAUDE.md).
