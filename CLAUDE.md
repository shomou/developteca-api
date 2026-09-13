# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Spring Boot 4.1 REST API (`developteca-api`) providing JWT-based authentication over a PostgreSQL database. Java 17, Maven build.

## Commands

```bash
# Build (compiles + runs tests)
mvn clean install

# Compile only (fast check for compile errors)
mvn compile

# Run the app
mvn spring-boot:run

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=ClassNameTest

# Run a single test method
mvn test -Dtest=ClassNameTest#methodName
```

The app expects a local PostgreSQL instance at `jdbc:postgresql://localhost:5432/developteca_db` (see `src/main/resources/application.yml`) and listens on port `8080`.

## Architecture

### Jackson version — Spring Boot 4 uses Jackson 3, not Jackson 2

This is the single most important gotcha in this codebase. Spring Boot 4.1's `spring-boot-starter-json` wires up **Jackson 3**, whose classes live under the `tools.jackson.*` package (e.g. `tools.jackson.databind.ObjectMapper`), not the classic `com.fasterxml.jackson.*` package from Jackson 2.

- The Spring-managed `ObjectMapper` bean (auto-configured with `LocalDateTime`/JSR-310 support) is of type `tools.jackson.databind.ObjectMapper`.
- `com.fasterxml.jackson.*` classes are still present on the classpath, but only because `jjwt-jackson` (used by the JWT library) pulls in Jackson 2's `jackson-databind` at **runtime scope** — it is not available at compile time and is a different, unrelated `ObjectMapper` type.
- Never instantiate a bare `new ObjectMapper()` for request/response serialization — always inject the Spring-managed `tools.jackson.databind.ObjectMapper` bean, otherwise types like `LocalDateTime` won't serialize (see `JwtAuthenticationEntryPoint`, which does this correctly).

### Auth flow

- `AuthController` (`/api/v1/auth`) exposes `register`, `login`, and `verify-email` (no longer a stub — see below). All three must stay in sync with the `permitAll()` matchers in `SecurityConfig` — a controller path that doesn't exactly match its `SecurityConfig` matcher falls through to `anyRequest().authenticated()` and returns 401 even with correct credentials (this happened once when `login` had no explicit `@PostMapping("/login")`).
- `AuthController` is also annotated `@CrossOrigin(origins = "*", maxAge = 3600)` at the class level — this is broader than, and conflicts with, the `localhost:4200`/`localhost:3000`-restricted CORS bean in `SecurityConfig`. Don't copy this pattern onto new controllers; it's an easy way to accidentally widen CORS.
- `AuthService` handles registration (hashes password via `PasswordEncoder`, defaults `role=USER`, `emailVerified=false`, generates a 24h-expiry email-verification token via `TokenUtil` and sends it through `EmailService`) and login (validates credentials + `ACTIVE` status, issues JWT access/refresh tokens — both are currently generated identically via `JwtService.generateToken`, with no distinct refresh-token claims/expiry). `verify-email` (`AuthService.verifyEmail`) looks up the user by token, checks expiry, clears the token, and saves — it's a real implementation now, not a stub.
- `EmailService` (uses `spring-boot-starter-mail` / `JavaMailSender`) sends the verification email and has a `sendPasswordResetEmail` method with no caller yet. `application.yml` now has a placeholder `spring.mail.*` block (`host: localhost`, `port: 1025`, dummy username/password) — without it, Spring Boot never creates a `JavaMailSender` bean and the app fails to start (`UnsatisfiedDependencyException` on `EmailService`'s constructor). The placeholder only exists to satisfy the bean; it points at nothing real, so sends will fail at runtime — but `EmailService` already wraps `mailSender.send(...)` in `catch (Exception e)` and just logs to `System.err`, so registration/login flows don't break. Swap in real SMTP credentials before verification emails need to actually deliver.
- `JwtService` (`io.jsonwebtoken` / JJWT 0.12) signs/parses tokens using an HMAC key derived from the `jwt.secret` property, with expiry from `jwt.expiration` (both in `application.yml`).
- `JwtAuthenticationFilter` (a `OncePerRequestFilter`) runs on every request, extracts the `Bearer` token, validates it via `JwtService`, loads the user via `UserDetailsServiceImpl`, and populates `SecurityContextHolder`.
- `SecurityConfig` wires the filter chain: stateless sessions, CSRF disabled (JWT-based), CORS restricted to `localhost:4200`/`localhost:3000`, `JwtAuthenticationEntryPoint` handles unauthenticated 401 responses, and public endpoints are explicitly listed (`/auth/register`, `/auth/login`, `/auth/verify-email`, `GET /articles*`, `/uploads/**`) — everything else requires authentication.
- `UserDetailsServiceImpl` maps a `User` entity to Spring Security's `UserDetails`, deriving the single granted authority from `Role` (prefixed `ROLE_`) and disabling the account unless `UserStatus == ACTIVE`.

### Articles — full CRUD + image upload surface is now wired up

`Article`, `Category`, `ArticleImage` entities (plus `ArticleStatus` enum: `DRAFT`/`PUBLISHED`/`ARCHIVED`) exist under `entity/`. The DTO layer (`dto/`): `ArticleSummaryResponse` (list view, no `content` field), `ArticleDetailResponse` (full view, includes `content` and `images`), `ArticleCreateRequest`/`ArticleUpdateRequest` (validated, Spanish messages — `status` defaults to `DRAFT` on create but is required on update), `ArticleImageResponse`, `CategoryResponse`, `AuthorResponse` (slim user projection). Repository layer: `ArticleRepository`, `CategoryRepository`, `ArticleImageRepository`.
- `ArticleRepository.findByPublishedArticles` is the public listing query — filters on `status = PUBLISHED` plus optional `categorySlug`/`search` (case-insensitive `LIKE` on title), paginated via `Pageable`. `findBySlug`/`existsBySlug` back slug-based lookup/uniqueness; `findByAuthor` is also paginated. **The `:search` parameter must stay wrapped in `CAST(:search AS string)`** inside `LOWER(CONCAT('%', ..., '%'))` — without the explicit cast, calling `GET /api/v1/articles` with no `search` param binds `:search` as `null` with no type info, and PostgreSQL's JDBC driver resolves the `CONCAT`/`||` ambiguity to `bytea` instead of `text`, throwing `function lower(bytea) does not exist` at query time. This is a PostgreSQL/pgjdbc quirk with untyped null parameters inside string-concatenation expressions, not a data problem — any new optional-string JPQL filter built the same way (`:param IS NULL OR LOWER(...) LIKE ...`) needs the same `CAST(... AS string)` treatment.
- `ArticleImageRepository.findByArticleIdAndIsFeaturedTrue` had a `fidnBy...` typo that used to silently resolve; it now throws `No property 'fidnByArticleId' found` at startup (Spring Data no longer parses `fidn` as a valid query-derivation prefix) and has been renamed to the correct `findBy...`. If a similarly-misspelled repository method ever fails to start the context, this is the failure shape to recognize.

`ArticleService` is annotated `@Service`, and `application.yml`'s `app.upload.allowed-types` key matches `ImageService`'s `@Value("${app.upload.allowed-types}")` — both were previously broken (missing bean + unresolved placeholder) and would have failed the app at startup; `mvn compile` succeeds and the context loads.

`ArticleController` (`/api/v1/articles`) now exposes the full surface, all wrapped in `ApiResponse` with per-endpoint `try/catch(Exception e)` (no central `@ControllerAdvice`):
- `GET /api/v1/articles` — paginated public list (`page`/`size`/`category`/`search`), 500 on error.
- `GET /api/v1/articles/{slug}` — public detail lookup via `ArticleService.getBySlug` (increments `viewsCount`), 404 on error.
- `POST /api/v1/articles` — create (`@Valid ArticleCreateRequest`), 201, 400 on error.
- `PUT /api/v1/articles/{id}` — update (`@Valid ArticleUpdateRequest`, backed by `ArticleService.update`), 200, 400 on error.
- `DELETE /api/v1/articles/{id}` — delete, 204, 400 on error.
- `POST /api/v1/articles/{id}/images` — multipart upload (`file`, `altText`, `isFeatured`, `orderIndex`), 201, 400 on error.
- `DELETE /api/v1/articles/{id}/images/{imageId}` — delete an image, 204, 400 on error.

All authenticated endpoints (everything but list/detail) call `SecurityUtil.getCurrentUser()` (renamed from the earlier `getCurrentuser()` typo) to resolve the acting `User`, then delegate ownership/role checks to `ArticleService.checkOwnershipOrAdmin` (author, or `Role.ADMIN`/`Role.SUPER_ADMIN`) — `SecurityConfig` itself has no role-based matchers, just `anyRequest().authenticated()` for anything not explicitly `permitAll()`'d. `SecurityConfig` now also permits `GET /api/v1/articles`, `GET /api/v1/articles/**`, and `/uploads/**` (matching `WebConfig`'s static resource handler for uploaded images).

Slugs are generated via `SlugUtil.toSlug()` + `ensureUniqueSlug` (appends `-1`, `-2`, ... on collision). **`SlugUtil`'s `WHITESPACE` pattern was inverted** — it was `[^\s]+` (matches non-whitespace runs) instead of `\s+` (matches whitespace runs), so any single-word input like `"Backend"` collapsed to a lone `-`, which the trailing `.replaceAll("^-|-$", "")` step then stripped to an **empty string**. Every slug came out `""`, and `DataSeeder` (a `CommandLineRunner` that seeds 5 default `Category` rows on first boot, skipped if `categoryRepository.count() > 0`) hit the `slug` unique constraint on the second insert and crashed startup. Now fixed to `\s+`; if slugs ever come back empty or collide unexpectedly, check this pattern first. `ImageService` (a `@Service`) stores uploaded files on local disk under `app.upload.dir` (`uploads/articles/{articleId}/{uuid}.{ext}`), validates size/content-type (throwing `InvalidImageException`), and `WebConfig` serves them back at `/uploads/**`. There is still no `spring.servlet.multipart.*` config in `application.yml`, so Spring's default multipart limits (1MB file / 10MB request) apply and are **smaller** than `app.upload.max-file-size` (5MB) — large uploads can get rejected before `ImageService` ever sees them. `ApiException` (message, or message+cause) is the general "not found"/business-rule exception used throughout `ArticleService`; `InvalidImageException` is specific to `ImageService` validation — both are still only ever caught by the generic `catch (Exception e)` blocks above, not typed per exception.

`commentsCount`/`averageRating` on `Article` are denormalized counters, not backed by real `Comment`/`Rating` entities.

### Entity auditing

`@PrePersist`/`@PreUpdate` lifecycle callbacks set `createdAt`/`updatedAt` — required whenever new timestamp-tracked entities are added, since `created_at` is `NOT NULL` at the DB level and Hibernate will not populate it automatically. **The existing entities are inconsistent, so don't copy any one of them blindly:**
- `User.onCreate()` sets only `createdAt` (leaves `updatedAt` null until the first update).
- `Article.onCreate()` and `Category.onCreate()` set **both** `createdAt` and `updatedAt`.
- `ArticleImage` has only `createdAt`/`@PrePersist` — no `updatedAt` column and no `@PreUpdate` at all.

### Response envelope

All controller responses are wrapped in `ApiResponse` (`success`, `message`, `data`, `timestamp`, `errors`), constructed manually in each controller method rather than via a global exception handler / `@ControllerAdvice`. Errors are currently caught with generic `catch (Exception e)` blocks per-endpoint rather than centrally.

### Configuration

`application.yml` indentation matters: `jwt`, `logging`, and `server` blocks must be **top-level** keys, not nested under `spring:` — nesting them there silently changes property paths (e.g. `jwt.secret` becomes `spring.jwt.secret`) and breaks `@Value("${jwt.secret}")` injection with a `PlaceholderResolutionException` at startup.

There's also a top-level `app.upload` block (`dir`, `max-file-size`, `allowed-types`) consumed by `ImageService` via `@Value`. No `spring.mail.*` or `spring.servlet.multipart.*` config exists yet, despite both `EmailService` and `ImageService` depending on related settings — see the multipart-size-limit gap noted above under Articles.

### IDE (VS Code) settings

`.vscode/settings.json` has `"java.compile.nullAnalysis.mode": "disabled"`. It was previously `"automatic"`, which made Eclipse JDT flag nearly every unbound instance method reference (`Article::getCreatedAt`, `ArticleImage::getIsFeatured`, etc.) with a false-positive "needs unchecked conversion to conform to '@NonNull X'" warning, since none of the codebase's classes carry null annotations. Don't re-enable it without also adopting null annotations project-wide, or the warnings come back everywhere.
