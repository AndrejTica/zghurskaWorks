package com.miravale.portfolio.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageStorageServiceTest {
    @TempDir
    Path uploadDirectory;

    @Test
    void storesAcceptedImageWithGeneratedFilename() throws Exception {
        ImageStorageService service = new ImageStorageService(uploadDirectory.toString());
        MockMultipartFile image = new MockMultipartFile("image", "portrait.png", "image/png", "pixels".getBytes());

        String filename = service.store(image);

        assertThat(filename).endsWith(".png").doesNotContain("portrait");
        assertThat(Files.readString(uploadDirectory.resolve(filename))).isEqualTo("pixels");
    }

    @Test
    void rejectsNonImageContent() {
        ImageStorageService service = new ImageStorageService(uploadDirectory.toString());
        MockMultipartFile file = new MockMultipartFile("image", "notes.txt", "text/plain", "text".getBytes());

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only JPEG, PNG, WebP, and GIF");
    }
}
