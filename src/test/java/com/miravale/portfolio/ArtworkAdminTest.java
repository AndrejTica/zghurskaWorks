package com.miravale.portfolio;

import com.miravale.portfolio.model.Artwork;
import com.miravale.portfolio.repository.ArtworkRepository;
import com.miravale.portfolio.service.ImageStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ArtworkAdminTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArtworkRepository artworkRepository;

    @Autowired
    private ImageStorageService imageStorageService;

    @AfterEach
    void cleanUpArtworks() {
        artworkRepository.findAll().forEach(artwork ->
                imageStorageService.delete(artwork.getImageFilename()));
        artworkRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updatesEveryArtworkFieldWithoutReplacingImage() throws Exception {
        Artwork artwork = createArtwork("original.png");

        mockMvc.perform(multipart("/admin/artworks/{id}", artwork.getId())
                        .param("title", "Updated title")
                        .param("description", "Updated description")
                        .param("createdDate", "2024-06-12")
                        .param("commissioned", "true")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));

        Artwork updated = artworkRepository.findById(artwork.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("Updated title");
        assertThat(updated.getDescription()).isEqualTo("Updated description");
        assertThat(updated.getCreatedDate()).isEqualTo(LocalDate.of(2024, 6, 12));
        assertThat(updated.isCommissioned()).isTrue();
        assertThat(updated.getImageFilename()).isEqualTo(artwork.getImageFilename());
        assertThat(Files.exists(Path.of("target/test-uploads", artwork.getImageFilename()))).isTrue();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void replacesArtworkImageAndDeletesPreviousFile() throws Exception {
        Artwork artwork = createArtwork("original.png");
        String previousFilename = artwork.getImageFilename();
        MockMultipartFile replacement = new MockMultipartFile(
                "image", "replacement.png", "image/png", "replacement".getBytes());

        mockMvc.perform(multipart("/admin/artworks/{id}", artwork.getId())
                        .file(replacement)
                        .param("title", artwork.getTitle())
                        .param("description", artwork.getDescription())
                        .param("createdDate", artwork.getCreatedDate().toString())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));

        Artwork updated = artworkRepository.findById(artwork.getId()).orElseThrow();
        assertThat(updated.getImageFilename()).isNotEqualTo(previousFilename);
        assertThat(Files.exists(Path.of("target/test-uploads", previousFilename))).isFalse();
        assertThat(Files.readString(Path.of("target/test-uploads", updated.getImageFilename())))
                .isEqualTo("replacement");
    }

    private Artwork createArtwork(String originalFilename) {
        MockMultipartFile image = new MockMultipartFile(
                "image", originalFilename, "image/png", "original".getBytes());
        Artwork artwork = new Artwork();
        artwork.setTitle("Original title");
        artwork.setDescription("Original description");
        artwork.setCreatedDate(LocalDate.of(2023, 1, 2));
        artwork.setImageFilename(imageStorageService.store(image));
        return artworkRepository.save(artwork);
    }
}
