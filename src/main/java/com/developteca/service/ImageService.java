package com.developteca.service;

import com.developteca.exception.InvalidImageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class ImageService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.upload.max-file-size}")
    private long maxFileSize;

    @Value("${app.upload.allowed-types}")
    private String allowedTypesRaw;

    // ============= VALIDAR Y GUARDAR IMAGEN =============
    public StoredImage store(MultipartFile file, Long articleId) {
        validateImage(file);

        try {
            // Crear carpeta: uploads/articles/{articleId}/
            Path articleDir = Paths.get(uploadDir, "articles", String.valueOf(articleId));
            Files.createDirectories(articleDir);

            // Generar nombre único
            String extension = getExtension(file.getOriginalFilename());
            String uniqueFilename = UUID.randomUUID() + extension;

            Path targetPath = articleDir.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // URL relativa (servida vía WebConfig, la configuramos después)
            String relativeUrl = "/uploads/articles/" + articleId + "/" + uniqueFilename;

            return new StoredImage(relativeUrl, targetPath.toString(), file.getSize(), file.getContentType());

        } catch (IOException e) {
            throw new RuntimeException("Error al guardar la imagen: " + e.getMessage(), e);
        }
    }

    // ============= ELIMINAR IMAGEN =============
    public void delete(String storagePath) {
        try {
            Path path = Paths.get(storagePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // Log pero no rompas el flujo si el archivo ya no existe
            System.err.println("Error eliminando imagen: " + e.getMessage());
        }
    }

    // ============= VALIDACIONES =============
    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImageException("El archivo de imagen está vacío");
        }

        if (file.getSize() > maxFileSize) {
            throw new InvalidImageException("La imagen supera el tamaño máximo de 5MB");
        }

        List<String> allowedTypes = List.of(allowedTypesRaw.split(","));
        if (!allowedTypes.contains(file.getContentType())) {
            throw new InvalidImageException("Formato de imagen no permitido. Usa JPG, PNG o WebP");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    // ============= CLASE INTERNA: RESULTADO DE ALMACENAMIENTO =============
    public static class StoredImage {
        private final String url;
        private final String storagePath;
        private final long fileSize;
        private final String mimeType;

        public StoredImage(String url, String storagePath, long fileSize, String mimeType) {
            this.url = url;
            this.storagePath = storagePath;
            this.fileSize = fileSize;
            this.mimeType = mimeType;
        }

        public String getUrl() { return url; }
        public String getStoragePath() { return storagePath; }
        public long getFileSize() { return fileSize; }
        public String getMimeType() { return mimeType; }
    }
}