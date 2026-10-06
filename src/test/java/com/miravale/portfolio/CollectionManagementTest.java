package com.miravale.portfolio;

import com.miravale.portfolio.model.ArtCollection;
import com.miravale.portfolio.model.Artwork;
import com.miravale.portfolio.model.SiteSettings;
import com.miravale.portfolio.repository.ArtCollectionRepository;
import com.miravale.portfolio.repository.ArtworkRepository;
import com.miravale.portfolio.repository.SiteSettingsRepository;
import com.miravale.portfolio.service.CollectionService;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CollectionManagementTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ArtCollectionRepository collections;
    @Autowired private ArtworkRepository artworks;
    @Autowired private SiteSettingsRepository settings;
    @Autowired private CollectionService collectionService;
    @Autowired private ImageStorageService imageStorageService;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        collections.findAll().forEach(collection -> collectionService.deleteCollection(collection.getId()));
        artworks.findAll().forEach(artwork -> imageStorageService.delete(artwork.getImageFilename()));
        artworks.deleteAll();
        settings.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void managerCreatesCollectionAssignsArtworkAndSelectsPreview() throws Exception {
        mockMvc.perform(post("/admin/collections").with(csrf())
                        .param("name", "Quiet places")
                        .param("description", "A".repeat(2000)))
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attributeExists("success"));
        ArtCollection collection = collections.findAll().get(0);
        assertThat(collection.getDescription()).hasSize(2000);

        mockMvc.perform(multipart("/admin/artworks")
                        .file(new MockMultipartFile("image", "art.png", "image/png", "image".getBytes()))
                        .with(csrf())
                        .param("title", "Morning")
                        .param("description", "Light on the water")
                        .param("createdDate", "2026-01-01")
                        .param("collectionId", collection.getId().toString()))
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("success", "Artwork added."));
        Artwork artwork = artworks.findAll().get(0);
        assertThat(artwork.getCollection().getId()).isEqualTo(collection.getId());

        mockMvc.perform(post("/admin/collections/{id}", collection.getId()).with(csrf())
                        .param("name", "Quiet places, revised")
                        .param("description", "First line\nSecond line")
                        .param("previewArtworkId", artwork.getId().toString()))
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("success", "Collection updated."));
        ArtCollection updated = collections.findById(collection.getId()).orElseThrow();
        assertThat(updated.getPreviewArtwork().getId()).isEqualTo(artwork.getId());
        assertThat(updated.getName()).isEqualTo("Quiet places, revised");
        assertThat(updated.getDescription()).isEqualTo("First line\nSecond line");

        String admin = page("/admin");
        assertThat(admin).contains("name=\"collectionId\"", "name=\"previewArtworkId\"", "Quiet places, revised");
        assertThat(panel(page("/"), "collections"))
                .contains("Quiet places, revised", "/collections/" + collection.getId(),
                        "/uploads/" + artwork.getImageFilename());
        assertThat(page("/collections/" + collection.getId()))
                .contains("First line\nSecond line", "Morning", "Light on the water", "Back to collections",
                        "href=\"/#collections\"", "href=\"/#commissions\"");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void onlyHighlightedCollectionAppearsOnHomepageAndCommissionsRemainSeparate() throws Exception {
        ArtCollection first = collection("First collection");
        ArtCollection second = collection("Second collection");
        Artwork older = artwork("Older painting", first, false);
        Artwork newer = artwork("Newer painting", first, true);
        newer.setCreatedDate(LocalDate.of(2026, 2, 1));
        artworks.save(newer);
        artwork("Other painting", second, false);
        artwork("Unassigned painting", null, false);
        artwork("Separate commission", second, true);

        assertThat(panel(page("/"), "works")).doesNotContain("Older painting", "Other painting", "Unassigned painting");
        highlight(first.getId());
        String home = page("/");
        assertThat(panel(home, "works"))
                .contains("First collection", "Older painting", "Newer painting")
                .doesNotContain("Other painting", "Unassigned painting", "Separate commission");
        assertThat(panel(home, "works").indexOf("Newer painting"))
                .isLessThan(panel(home, "works").indexOf("Older painting"));
        assertThat(panel(home, "commissions")).contains("Newer painting", "Separate commission")
                .doesNotContain("Older painting");
        assertThat(page("/collections/" + first.getId()))
                .contains("Older painting", "Newer painting")
                .doesNotContain("Other painting", "Unassigned painting", "Separate commission");
        assertThat(artworks.findById(older.getId())).isPresent();

        highlight(second.getId());
        assertThat(panel(page("/"), "works")).contains("Other painting", "Separate commission")
                .doesNotContain("Older painting", "Newer painting");
        mockMvc.perform(post("/admin/highlighted-collection").with(csrf()).param("collectionId", ""))
                .andExpect(flash().attributeExists("success"));
        assertThat(settings.findById(SiteSettings.ID).orElseThrow().getHighlightedCollection()).isNull();
        assertThat(panel(page("/"), "works")).doesNotContain("Other painting", "Separate commission");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsPreviewOutsideCollectionWithoutChangingCollection() throws Exception {
        ArtCollection first = collection("First");
        ArtCollection second = collection("Second");
        Artwork own = artwork("Own preview", first, false);
        Artwork foreign = artwork("Foreign preview", second, false);
        collectionService.updateCollection(first.getId(), first.getName(), first.getDescription(), own.getId());

        mockMvc.perform(post("/admin/collections/{id}", first.getId()).with(csrf())
                        .param("name", "Must not persist")
                        .param("description", "Must not persist")
                        .param("previewArtworkId", foreign.getId().toString()))
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("error", "The preview artwork must belong to this collection."));
        ArtCollection unchanged = collections.findById(first.getId()).orElseThrow();
        assertThat(unchanged.getName()).isEqualTo("First");
        assertThat(unchanged.getPreviewArtwork().getId()).isEqualTo(own.getId());
        String admin = page("/admin");
        String previewOptions = admin.substring(admin.indexOf("id=\"edit-collection-preview-" + first.getId() + "\""));
        assertThat(previewOptions.substring(0, previewOptions.indexOf("</select>")))
                .contains("Own preview").doesNotContain("Foreign preview");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void movingAndUnassigningArtworkMaintainsSingleMembershipAndClearsOldPreview() throws Exception {
        ArtCollection first = collection("First");
        ArtCollection second = collection("Second");
        Artwork artwork = artwork("Moving artwork", first, false);
        collectionService.updateCollection(first.getId(), first.getName(), first.getDescription(), artwork.getId());

        mockMvc.perform(multipart("/admin/artworks/{id}", artwork.getId()).with(csrf())
                        .param("title", artwork.getTitle()).param("description", artwork.getDescription())
                        .param("createdDate", artwork.getCreatedDate().toString())
                        .param("collectionId", second.getId().toString()))
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("success", containsString("Collection preview cleared")));
        assertThat(artworks.findById(artwork.getId()).orElseThrow().getCollection().getId()).isEqualTo(second.getId());
        assertThat(collections.findById(first.getId()).orElseThrow().getPreviewArtwork()).isNull();
        assertThat(page("/collections/" + first.getId())).doesNotContain("Moving artwork");
        assertThat(page("/collections/" + second.getId())).contains("Moving artwork");

        mockMvc.perform(multipart("/admin/artworks/{id}", artwork.getId()).with(csrf())
                        .param("title", artwork.getTitle()).param("description", artwork.getDescription())
                        .param("createdDate", artwork.getCreatedDate().toString()).param("collectionId", ""))
                .andExpect(flash().attribute("success", "Artwork updated."));
        assertThat(artworks.findById(artwork.getId()).orElseThrow().getCollection()).isNull();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void replacingPreviewImageRetainsSelectionAndUpdatesPublicPreview() throws Exception {
        ArtCollection collection = collection("Preview collection");
        Artwork artwork = artwork("Original preview", collection, false);
        collectionService.updateCollection(collection.getId(), collection.getName(), collection.getDescription(), artwork.getId());

        mockMvc.perform(multipart("/admin/artworks/{id}", artwork.getId())
                        .file(new MockMultipartFile("image", "replacement.png", "image/png", "replacement".getBytes()))
                        .param("title", "Updated preview").param("description", artwork.getDescription())
                        .param("createdDate", artwork.getCreatedDate().toString())
                        .param("collectionId", collection.getId().toString()).with(csrf()))
                .andExpect(flash().attribute("success", "Artwork updated."));
        Artwork updated = artworks.findById(artwork.getId()).orElseThrow();
        assertThat(collections.findById(collection.getId()).orElseThrow().getPreviewArtwork().getId())
                .isEqualTo(artwork.getId());
        assertThat(panel(page("/"), "collections"))
                .contains("/uploads/" + updated.getImageFilename(), "Updated preview")
                .doesNotContain(artwork.getImageFilename());
        assertThat(Files.exists(Path.of("target/test-uploads", artwork.getImageFilename()))).isFalse();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidArtworkMoveLeavesMembershipAndPreviewUnchanged() throws Exception {
        ArtCollection collection = collection("Original collection");
        Artwork artwork = artwork("Original artwork", collection, false);
        collectionService.updateCollection(collection.getId(), collection.getName(), collection.getDescription(), artwork.getId());

        mockMvc.perform(multipart("/admin/artworks/{id}", artwork.getId()).with(csrf())
                        .param("title", "Invalid change").param("description", artwork.getDescription())
                        .param("createdDate", artwork.getCreatedDate().toString())
                        .param("collectionId", Long.toString(Long.MAX_VALUE)))
                .andExpect(flash().attribute("error", "Collection no longer exists."));
        Artwork unchanged = artworks.findById(artwork.getId()).orElseThrow();
        assertThat(unchanged.getTitle()).isEqualTo("Original artwork");
        assertThat(unchanged.getCollection().getId()).isEqualTo(collection.getId());
        assertThat(collections.findById(collection.getId()).orElseThrow().getPreviewArtwork().getId())
                .isEqualTo(artwork.getId());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deletingHighlightedCollectionKeepsArtworkAndImage() throws Exception {
        ArtCollection collection = collection("Temporary collection");
        Artwork artwork = artwork("Kept artwork", collection, false);
        collectionService.updateCollection(collection.getId(), collection.getName(), collection.getDescription(), artwork.getId());
        highlight(collection.getId());

        mockMvc.perform(post("/admin/collections/{id}/delete", collection.getId()).with(csrf()))
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attributeExists("success"));
        assertThat(collections.findById(collection.getId())).isEmpty();
        assertThat(artworks.findById(artwork.getId()).orElseThrow().getCollection()).isNull();
        assertThat(settings.findById(SiteSettings.ID).orElseThrow().getHighlightedCollection()).isNull();
        assertThat(Files.exists(Path.of("target/test-uploads", artwork.getImageFilename()))).isTrue();
        mockMvc.perform(get("/collections/{id}", collection.getId())).andExpect(status().isNotFound());
        assertThat(page("/")).doesNotContain("Temporary collection");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deletingPreviewArtworkClearsPreviewAndRemovesFile() throws Exception {
        ArtCollection collection = collection("Collection");
        Artwork artwork = artwork("Preview", collection, false);
        collectionService.updateCollection(collection.getId(), collection.getName(), collection.getDescription(), artwork.getId());

        mockMvc.perform(post("/admin/artworks/{id}/delete", artwork.getId()).with(csrf()))
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("success", containsString("Collection preview cleared")));
        assertThat(collections.findById(collection.getId()).orElseThrow().getPreviewArtwork()).isNull();
        assertThat(artworks.findById(artwork.getId())).isEmpty();
        assertThat(Files.exists(Path.of("target/test-uploads", artwork.getImageFilename()))).isFalse();
        assertThat(panel(page("/"), "collections")).contains("Preview coming soon");
        assertThat(page("/collections/" + collection.getId())).contains("Artworks will be added");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidAndStaleAdminActionsReportErrorsWithoutSaving() throws Exception {
        mockMvc.perform(post("/admin/collections").with(csrf()).param("name", " ").param("description", "Description"))
                .andExpect(flash().attributeExists("error"));
        mockMvc.perform(post("/admin/collections").with(csrf()).param("name", "A".repeat(151))
                        .param("description", "A".repeat(2001)))
                .andExpect(flash().attributeExists("error"));
        assertThat(collections.count()).isZero();
        ArtCollection collection = collection("Valid");
        highlight(collection.getId());
        mockMvc.perform(post("/admin/collections/{id}", collection.getId()).with(csrf())
                        .param("name", "Invalid edit").param("description", " "))
                .andExpect(flash().attributeExists("error"));
        assertThat(collections.findById(collection.getId()).orElseThrow().getName()).isEqualTo("Valid");
        mockMvc.perform(post("/admin/collections/{id}", collection.getId()).with(csrf())
                        .param("name", "Missing preview").param("description", "Description")
                        .param("previewArtworkId", Long.toString(Long.MAX_VALUE)))
                .andExpect(flash().attribute("error", "Preview artwork no longer exists."));
        mockMvc.perform(post("/admin/collections/{id}", Long.MAX_VALUE).with(csrf())
                        .param("name", "Missing").param("description", "Description"))
                .andExpect(flash().attribute("error", "Collection no longer exists."));
        mockMvc.perform(post("/admin/collections/{id}/delete", Long.MAX_VALUE).with(csrf()))
                .andExpect(flash().attribute("error", "Collection no longer exists."));
        mockMvc.perform(post("/admin/highlighted-collection").with(csrf())
                        .param("collectionId", Long.toString(Long.MAX_VALUE)))
                .andExpect(flash().attribute("error", "Collection no longer exists."));
        assertThat(settings.findById(SiteSettings.ID).orElseThrow().getHighlightedCollection().getId())
                .isEqualTo(collection.getId());
        mockMvc.perform(multipart("/admin/artworks")
                        .file(new MockMultipartFile("image", "art.png", "image/png", "image".getBytes()))
                        .with(csrf())
                        .param("title", "Invalid").param("description", "Invalid")
                        .param("createdDate", "2026-01-01").param("collectionId", Long.toString(Long.MAX_VALUE)))
                .andExpect(status().isOk()).andExpect(model().hasErrors())
                .andExpect(content().string(containsString("Collection no longer exists.")));
        assertThat(artworks.count()).isZero();
    }

    @Test
    void publicEmptyCollectionsAndProtectedMutations() throws Exception {
        assertThat(panel(page("/"), "collections")).contains("Collections are being prepared");
        ArtCollection collection = collection("Empty collection");
        assertThat(page("/collections/" + collection.getId())).contains("Empty collection", "Artworks will be added");
        mockMvc.perform(post("/admin/collections").with(csrf())
                        .param("name", "Unauthorized").param("description", "Unauthorized"))
                .andExpect(status().is3xxRedirection());
        assertThat(collections.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void collectionMutationsRequireCsrf() throws Exception {
        ArtCollection collection = collection("Protected");
        mockMvc.perform(post("/admin/collections").param("name", "New").param("description", "Description"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/collections/{id}", collection.getId())
                        .param("name", "Changed").param("description", "Description"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/collections/{id}/delete", collection.getId()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/highlighted-collection").param("collectionId", collection.getId().toString()))
                .andExpect(status().isForbidden());
    }

    private ArtCollection collection(String name) {
        ArtCollection collection = new ArtCollection();
        collection.setName(name);
        collection.setDescription(name + " description");
        return collections.save(collection);
    }

    private Artwork artwork(String title, ArtCollection collection, boolean commissioned) {
        Artwork artwork = new Artwork();
        artwork.setTitle(title);
        artwork.setDescription(title + " description");
        artwork.setCreatedDate(LocalDate.of(2026, 1, 1));
        artwork.setImageFilename(imageStorageService.store(
                new MockMultipartFile("image", "art.png", "image/png", "image".getBytes())));
        artwork.setCommissioned(commissioned);
        artwork.setCollection(collection);
        return artworks.save(artwork);
    }

    private void highlight(Long id) throws Exception {
        mockMvc.perform(post("/admin/highlighted-collection").with(csrf()).param("collectionId", id.toString()))
                .andExpect(redirectedUrl("/admin")).andExpect(flash().attributeExists("success"));
    }

    private String page(String path) throws Exception {
        return mockMvc.perform(get(path)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private String panel(String html, String id) {
        int start = html.indexOf("<section id=\"" + id + "\"");
        assertThat(start).isNotNegative();
        return html.substring(start, html.indexOf("</section>", start));
    }
}
