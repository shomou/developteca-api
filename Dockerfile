# ============================================================
# Etapa 1: compilación
# Necesita Maven y el JDK completo, pero nada de esto llega a la imagen final.
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# Copiar solo el pom primero y descargar dependencias en su propia capa:
# mientras el pom no cambie, Docker reutiliza esta capa y no vuelve a bajar
# medio Maven Central en cada build por un cambio en el código.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ============================================================
# Etapa 2: ejecución
# Solo el JRE y el jar. Sin Maven, sin JDK, sin código fuente.
# ============================================================
FROM eclipse-temurin:17-jre

# Usuario sin privilegios: si alguien logra ejecutar algo dentro del contenedor,
# no lo hace como root.
RUN groupadd --system spring && useradd --system --gid spring spring

WORKDIR /app

# Directorio de imágenes subidas (app.upload.dir). En compose se monta un volumen
# encima para que sobreviva a la reconstrucción de la imagen.
RUN mkdir -p /app/uploads && chown -R spring:spring /app

COPY --from=build --chown=spring:spring /build/target/*.jar app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
