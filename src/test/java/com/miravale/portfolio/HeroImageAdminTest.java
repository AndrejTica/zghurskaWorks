package com.miravale.portfolio;

import com.miravale.portfolio.model.SiteSettings;
import com.miravale.portfolio.repository.SiteSettingsRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HeroImageAdminTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SiteSettingsRepository siteSettingsRepository;

    @Autowired
    private ImageStorageService imageStorageService;

    @AfterEach
    void cleanUpHeroImage() {
        siteSettingsRepository.findById(SiteSettings.ID).ifPresent(settings ->
                imageStorageService.delete(settings.getHeroImageFilename()));
        siteSettingsRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void replacesLandingPageImageAndDeletesPreviousFile() throws Exception {
        uploadHeroImage("first.png", "first image");
        String firstFilename = currentHeroFilename();

        uploadHeroImage("replacement.png", "replacement image");
        String replacementFilename = currentHeroFilename();

        assertThat(replacementFilename).isNotEqualTo(firstFilename);
        assertThat(Files.exists(Path.of("target/test-uploads", firstFilename))).isFalse();
        assertThat(Files.readString(Path.of("target/test-uploads", replacementFilename)))
                .isEqualTo("replacement image");
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "/uploads/" + replacementFilename)));
    }

    private void uploadHeroImage(String originalFilename, String contents) throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "heroImage",
                originalFilename,
                "image/png",
                contents.getBytes());

        mockMvc.perform(multipart("/admin/hero-image").file(image).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    private String currentHeroFilename() {
        return siteSettingsRepository.findById(SiteSettings.ID)
                .orElseThrow()
                .getHeroImageFilename();
    }
}
