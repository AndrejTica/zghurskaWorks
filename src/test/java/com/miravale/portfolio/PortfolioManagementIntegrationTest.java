package com.miravale.portfolio;

import com.miravale.portfolio.model.Artwork;
import com.miravale.portfolio.model.Exhibition;
import com.miravale.portfolio.repository.ArtworkRepository;
import com.miravale.portfolio.repository.ExhibitionRepository;
import com.miravale.portfolio.service.ImageStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PortfolioManagementIntegrationTest {
    private static final Path UPLOAD_DIRECTORY = Path.of("target/test-uploads");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArtworkRepository artworkRepository;

    @Autowired
    private ExhibitionRepository exhibitionRepository;

    @Autowired
    private ImageStorageService imageStorageService;

    @BeforeEach
    @AfterEach
    void cleanUpPortfolioRecords() {
        artworkRepository.findAll().forEach(artwork ->
                imageStorageService.delete(artwork.getImageFilename()));
        artworkRepository.deleteAll();
        exhibitionRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void artworkCanBeCreatedEditedAndDeletedThroughTheAdminFlow() throws Exception {
        MockMultipartFile originalImage = new MockMultipartFile(
                "image", "original.png", "image/png", "original image".getBytes());

        mockMvc.perform(multipart("/admin/artworks")
                        .file(originalImage)
                        .param("title", "Blue Morning")
                        .param("description", "Oil and pigment on linen")
                        .param("createdDate", "2025-03-14")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("success", "Artwork added."));

        Artwork created = artworkRepository.findAll().stream().findFirst().orElseThrow();
        Path originalFile = UPLOAD_DIRECTORY.resolve(created.getImageFilename());
        assertThat(Files.readString(originalFile)).isEqualTo("original image");

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Blue Morning")))
                .andExpect(content().string(containsString("Save changes")));
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Blue Morning")))
                .andExpect(content().string(containsString("Oil and pigment on linen")));

        MockMultipartFile replacementImage = new MockMultipartFile(
                "image", "replacement.webp", "image/webp", "replacement image".getBytes());
        mockMvc.perform(multipart("/admin/artworks/{id}", created.getId())
                        .file(replacementImage)
                        .param("title", "Blue Morning, Revised")
                        .param("description", "Mixed media on linen")
                        .param("createdDate", "2026-08-22")
                        .param("commissioned", "true")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("success", "Artwork updated."));

        Artwork updated = artworkRepository.findById(created.getId()).orElseThrow();
        Path replacementFile = UPLOAD_DIRECTORY.resolve(updated.getImageFilename());
        assertThat(updated.getTitle()).isEqualTo("Blue Morning, Revised");
        assertThat(updated.getDescription()).isEqualTo("Mixed media on linen");
        assertThat(updated.getCreatedDate()).isEqualTo(LocalDate.of(2026, 8, 22));
        assertThat(updated.isCommissioned()).isTrue();
        assertThat(Files.exists(originalFile)).isFalse();
        assertThat(Files.readString(replacementFile)).isEqualTo("replacement image");

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Blue Morning, Revised")))
                .andExpect(content().string(containsString("Mixed media on linen")))
                .andExpect(content().string(not(containsString("Oil and pigment on linen"))));

        mockMvc.perform(post("/admin/artworks/{id}/delete", updated.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("success", "Artwork deleted."));

        assertThat(artworkRepository.findById(updated.getId())).isEmpty();
        assertThat(Files.exists(replacementFile)).isFalse();
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Blue Morning, Revised"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void exhibitionCanBeCreatedPublishedAndDeletedThroughTheAdminFlow() throws Exception {
        LocalDate startDate = LocalDate.now().plusMonths(1);
        LocalDate endDate = startDate.plusWeeks(3);

        mockMvc.perform(post("/admin/exhibitions")
                        .param("title", "Echoes in Colour")
                        .param("venue", "North Gallery")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .param("details", "A new collection of abstract works.")
                        .param("difficulty", "4")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("success", "Exhibition added."));

        Exhibition exhibition = exhibitionRepository.findAll().stream().findFirst().orElseThrow();
        assertThat(exhibition.getTitle()).isEqualTo("Echoes in Colour");
        assertThat(exhibition.getVenue()).isEqualTo("North Gallery");
        assertThat(exhibition.getStartDate()).isEqualTo(startDate);
        assertThat(exhibition.getEndDate()).isEqualTo(endDate);
        assertThat(exhibition.getDifficulty()).isEqualTo(4);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Echoes in Colour")))
                .andExpect(content().string(containsString("North Gallery")))
                .andExpect(content().string(containsString("A new collection of abstract works.")))
                .andExpect(content().string(containsString("★★★★☆")))
                .andExpect(content().string(containsString("Upcoming")));

        mockMvc.perform(post("/admin/exhibitions/{id}/delete", exhibition.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("success", "Exhibition deleted."));

        assertThat(exhibitionRepository.findById(exhibition.getId())).isEmpty();
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Echoes in Colour"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void exhibitionDefaultsToNoDifficultyAndDoesNotDisplayARating() throws Exception {
        LocalDate startDate = LocalDate.now().plusMonths(2);

        mockMvc.perform(post("/admin/exhibitions")
                        .param("title", "Untitled Spaces")
                        .param("venue", "Studio Annex")
                        .param("startDate", startDate.toString())
                        .param("endDate", startDate.plusDays(10).toString())
                        .param("details", "An exhibition without a difficulty rating.")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("success", "Exhibition added."));

        Exhibition exhibition = exhibitionRepository.findAll().stream().findFirst().orElseThrow();
        assertThat(exhibition.getDifficulty()).isNull();

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Untitled Spaces")))
                .andExpect(content().string(not(containsString("class=\"stars\""))));
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Untitled Spaces")))
                .andExpect(content().string(not(containsString("class=\"difficulty\""))));
    }
}
