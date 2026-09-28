package com.miravale.portfolio.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ImageStorageService {
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );
    private static final Set<String> ALLOWED_TYPES = EXTENSIONS.keySet();

    private final Path uploadDirectory;

    public ImageStorageService(@Value("${app.upload-dir}") String uploadDirectory) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile image) {
        if (image.isEmpty()) {
            throw new IllegalArgumentException("Please choose an image.");
        }

        String contentType = StringUtils.hasText(image.getContentType()) ? image.getContentType() : "";
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only JPEG, PNG, WebP, and GIF images are accepted.");
        }

        String filename = UUID.randomUUID() + EXTENSIONS.get(contentType);
        Path destination = uploadDirectory.resolve(filename).normalize();
        if (!destination.getParent().equals(uploadDirectory)) {
            throw new IllegalArgumentException("Invalid image filename.");
        }

        try {
            Files.createDirectories(uploadDirectory);
            try (InputStream input = image.getInputStream()) {
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return filename;
        } catch (IOException exception) {
            throw new IllegalStateException("The image could not be saved.", exception);
        }
    }

    public void delete(String filename) {
        if (!StringUtils.hasText(filename)) {
            return;
        }
        Path file = uploadDirectory.resolve(filename).normalize();
        if (!file.getParent().equals(uploadDirectory)) {
            throw new IllegalArgumentException("Invalid image filename.");
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            throw new IllegalStateException("The image could not be deleted.", exception);
        }
    }
}
