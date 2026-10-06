package com.miravale.portfolio.controller;

import com.miravale.portfolio.model.Artwork;
import com.miravale.portfolio.model.ArtCollection;
import com.miravale.portfolio.model.Exhibition;
import com.miravale.portfolio.model.SiteSettings;
import com.miravale.portfolio.repository.ArtworkRepository;
import com.miravale.portfolio.repository.ArtCollectionRepository;
import com.miravale.portfolio.repository.ExhibitionRepository;
import com.miravale.portfolio.repository.SiteSettingsRepository;
import com.miravale.portfolio.service.ImageStorageService;
import com.miravale.portfolio.service.CollectionService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {
    private final ArtworkRepository artworkRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final SiteSettingsRepository siteSettingsRepository;
    private final ImageStorageService imageStorageService;
    private final ArtCollectionRepository collectionRepository;
    private final CollectionService collectionService;

    public AdminController(
            ArtworkRepository artworkRepository,
            ExhibitionRepository exhibitionRepository,
            SiteSettingsRepository siteSettingsRepository,
            ImageStorageService imageStorageService,
            ArtCollectionRepository collectionRepository,
            CollectionService collectionService) {
        this.artworkRepository = artworkRepository;
        this.exhibitionRepository = exhibitionRepository;
        this.siteSettingsRepository = siteSettingsRepository;
        this.imageStorageService = imageStorageService;
        this.collectionRepository = collectionRepository;
        this.collectionService = collectionService;
    }

    @InitBinder("artwork")
    void artworkFields(WebDataBinder binder) {
        binder.setAllowedFields("title", "description", "createdDate", "commissioned");
    }

    @GetMapping("/admin")
    String admin(Model model) {
        if (!model.containsAttribute("artwork")) {
            model.addAttribute("artwork", new Artwork());
        }
        if (!model.containsAttribute("exhibition")) {
            model.addAttribute("exhibition", new Exhibition());
        }
        addLists(model);
        return "admin";
    }

    @PostMapping("/admin/artworks")
    String createArtwork(
            @Valid @ModelAttribute Artwork artwork,
            BindingResult bindingResult,
            @RequestParam("image") MultipartFile image,
            @RequestParam(required = false) Long collectionId,
            Model model,
            RedirectAttributes redirectAttributes) {
        model.addAttribute("selectedCollectionId", collectionId);
        try {
            artwork.setCollection(collectionService.findCollection(collectionId));
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("collection.invalid", exception.getMessage());
        }
        if (image.isEmpty()) {
            bindingResult.rejectValue("imageFilename", "image.required", "Please choose an image.");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("exhibition", new Exhibition());
            addLists(model);
            return "admin";
        }

        String filename;
        try {
            filename = imageStorageService.store(image);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            bindingResult.rejectValue("imageFilename", "image.invalid", exception.getMessage());
            model.addAttribute("exhibition", new Exhibition());
            addLists(model);
            return "admin";
        }

        artwork.setImageFilename(filename);
        try {
            collectionService.saveArtwork(artwork);
        } catch (RuntimeException exception) {
            imageStorageService.delete(filename);
            throw exception;
        }
        redirectAttributes.addFlashAttribute("success", "Artwork added.");
        return "redirect:/admin";
    }

    @PostMapping("/admin/exhibitions")
    String createExhibition(
            @Valid @ModelAttribute Exhibition exhibition,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (exhibition.getStartDate() != null && exhibition.getEndDate() != null
                && exhibition.getEndDate().isBefore(exhibition.getStartDate())) {
            bindingResult.rejectValue("endDate", "date.order", "End date must be on or after the start date.");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("artwork", new Artwork());
            addLists(model);
            return "admin";
        }
        exhibitionRepository.save(exhibition);
        redirectAttributes.addFlashAttribute("success", "Exhibition added.");
        return "redirect:/admin";
    }

    @PostMapping("/admin/hero-image")
    String replaceHeroImage(
            @RequestParam("heroImage") MultipartFile heroImage,
            RedirectAttributes redirectAttributes) {
        if (heroImage.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please choose a hero image.");
            return "redirect:/admin";
        }

        String newFilename;
        try {
            newFilename = imageStorageService.store(heroImage);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/admin";
        }

        SiteSettings settings = siteSettingsRepository.findById(SiteSettings.ID).orElseGet(SiteSettings::new);
        String previousFilename = settings.getHeroImageFilename();
        settings.setHeroImageFilename(newFilename);
        try {
            siteSettingsRepository.save(settings);
        } catch (RuntimeException exception) {
            imageStorageService.delete(newFilename);
            throw exception;
        }

        try {
            imageStorageService.delete(previousFilename);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Hero image replaced, but the previous image file could not be removed.");
            return "redirect:/admin";
        }

        redirectAttributes.addFlashAttribute("success", "Hero image replaced.");
        return "redirect:/admin";
    }

    @PostMapping("/admin/landing-content")
    String updateLandingPageContent(
            @RequestParam("landingPageSlogan") String landingPageSlogan,
            @RequestParam("landingPageDescription") String landingPageDescription,
            @RequestParam("studioAddress") String studioAddress,
            RedirectAttributes redirectAttributes) {
        String slogan = landingPageSlogan.trim();
        String description = landingPageDescription.trim();
        String address = studioAddress.trim();
        if (!StringUtils.hasText(slogan)
                || !StringUtils.hasText(description)
                || !StringUtils.hasText(address)) {
            addLandingContentError(
                    redirectAttributes,
                    slogan,
                    description,
                    address,
                    "Slogan, description, and studio address are required.");
            return "redirect:/admin";
        }
        if (slogan.length() > 300 || description.length() > 2000 || address.length() > 500) {
            addLandingContentError(
                    redirectAttributes,
                    slogan,
                    description,
                    address,
                    "Landing page text exceeds the allowed length.");
            return "redirect:/admin";
        }

        SiteSettings settings = siteSettingsRepository.findById(SiteSettings.ID).orElseGet(SiteSettings::new);
        settings.setLandingPageSlogan(slogan);
        settings.setLandingPageDescription(description);
        settings.setStudioAddress(address);
        siteSettingsRepository.save(settings);
        redirectAttributes.addFlashAttribute("success", "Landing page text updated.");
        return "redirect:/admin";
    }

    @PostMapping("/admin/artworks/{id}/delete")
    String deleteArtwork(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Artwork artwork = artworkRepository.findById(id).orElse(null);
        if (artwork == null) {
            redirectAttributes.addFlashAttribute("error", "Artwork no longer exists.");
            return "redirect:/admin";
        }
        boolean previewCleared = collectionService.deleteArtwork(artwork);
        try {
            imageStorageService.delete(artwork.getImageFilename());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Artwork deleted, but its image file could not be removed.");
            return "redirect:/admin";
        }
        redirectAttributes.addFlashAttribute("success", "Artwork deleted." + previewNotice(previewCleared));
        return "redirect:/admin";
    }

    @PostMapping("/admin/artworks/{id}")
    String updateArtwork(
            @PathVariable Long id,
            @Valid @ModelAttribute Artwork changes,
            BindingResult bindingResult,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(required = false) Long collectionId,
            RedirectAttributes redirectAttributes) {
        Artwork artwork = artworkRepository.findById(id).orElse(null);
        if (artwork == null) {
            redirectAttributes.addFlashAttribute("error", "Artwork no longer exists.");
            return "redirect:/admin";
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Artwork was not updated. Please complete all fields and check their lengths.");
            return "redirect:/admin";
        }

        ArtCollection collection;
        try {
            collection = collectionService.findCollection(collectionId);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/admin";
        }

        String previousFilename = artwork.getImageFilename();
        String newFilename = null;
        if (image != null && !image.isEmpty()) {
            try {
                newFilename = imageStorageService.store(image);
            } catch (IllegalArgumentException | IllegalStateException exception) {
                redirectAttributes.addFlashAttribute("error", exception.getMessage());
                return "redirect:/admin";
            }
        }

        artwork.setTitle(changes.getTitle());
        artwork.setDescription(changes.getDescription());
        artwork.setCreatedDate(changes.getCreatedDate());
        artwork.setCommissioned(changes.isCommissioned());
        artwork.setCollection(collection);
        if (newFilename != null) {
            artwork.setImageFilename(newFilename);
        }

        boolean previewCleared;
        try {
            previewCleared = collectionService.saveArtwork(artwork);
        } catch (RuntimeException exception) {
            imageStorageService.delete(newFilename);
            throw exception;
        }

        if (newFilename != null) {
            try {
                imageStorageService.delete(previousFilename);
            } catch (IllegalArgumentException | IllegalStateException exception) {
                redirectAttributes.addFlashAttribute(
                        "error",
                        "Artwork updated, but the previous image file could not be removed.");
                return "redirect:/admin";
            }
        }
        redirectAttributes.addFlashAttribute("success", "Artwork updated." + previewNotice(previewCleared));
        return "redirect:/admin";
    }

    @PostMapping("/admin/exhibitions/{id}/delete")
    String deleteExhibition(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (!exhibitionRepository.existsById(id)) {
            redirectAttributes.addFlashAttribute("error", "Exhibition no longer exists.");
            return "redirect:/admin";
        }
        exhibitionRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Exhibition deleted.");
        return "redirect:/admin";
    }

    @PostMapping("/admin/exhibitions/{id}")
    String updateExhibition(
            @PathVariable Long id,
            @Valid @ModelAttribute Exhibition changes,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        Exhibition exhibition = exhibitionRepository.findById(id).orElse(null);
        if (exhibition == null) {
            redirectAttributes.addFlashAttribute("error", "Exhibition no longer exists.");
            return "redirect:/admin";
        }
        if (changes.getStartDate() != null && changes.getEndDate() != null
                && changes.getEndDate().isBefore(changes.getStartDate())) {
            bindingResult.rejectValue("endDate", "date.order", "End date must be on or after the start date.");
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Exhibition was not updated. Please complete all fields and check the dates.");
            return "redirect:/admin";
        }

        exhibition.setTitle(changes.getTitle());
        exhibition.setVenue(changes.getVenue());
        exhibition.setStartDate(changes.getStartDate());
        exhibition.setEndDate(changes.getEndDate());
        exhibition.setDetails(changes.getDetails());
        exhibition.setDifficulty(changes.getDifficulty());
        exhibitionRepository.save(exhibition);
        redirectAttributes.addFlashAttribute("success", "Exhibition updated.");
        return "redirect:/admin";
    }

    private void addLists(Model model) {
        if (!model.containsAttribute("artCollection")) {
            model.addAttribute("artCollection", new ArtCollection());
        }
        model.addAttribute("collections", collectionRepository.findAllByOrderByNameAscIdAsc());
        model.addAttribute("artworks", artworkRepository.findAll());
        model.addAttribute("exhibitions", exhibitionRepository.findAllByOrderByStartDateAsc());
        SiteSettings settings = siteSettingsRepository.findById(SiteSettings.ID).orElseGet(SiteSettings::new);
        model.addAttribute("highlightedCollection", settings.getHighlightedCollection());
        model.addAttribute("heroImageFilename", settings.getHeroImageFilename());
        if (!model.containsAttribute("landingPageSlogan")) {
            model.addAttribute(
                    "landingPageSlogan",
                    StringUtils.hasText(settings.getLandingPageSlogan())
                            ? settings.getLandingPageSlogan()
                            : SiteSettings.DEFAULT_LANDING_PAGE_SLOGAN);
        }
        if (!model.containsAttribute("landingPageDescription")) {
            model.addAttribute(
                    "landingPageDescription",
                    StringUtils.hasText(settings.getLandingPageDescription())
                            ? settings.getLandingPageDescription()
                            : SiteSettings.DEFAULT_LANDING_PAGE_DESCRIPTION);
        }
        if (!model.containsAttribute("studioAddress")) {
            model.addAttribute(
                    "studioAddress",
                    StringUtils.hasText(settings.getStudioAddress())
                            ? settings.getStudioAddress()
                            : SiteSettings.DEFAULT_STUDIO_ADDRESS);
        }
    }

    private String previewNotice(boolean previewCleared) {
        return previewCleared ? " Collection preview cleared; please choose a new preview in Collections." : "";
    }

    private void addLandingContentError(
            RedirectAttributes redirectAttributes,
            String slogan,
            String description,
            String address,
            String errorMessage) {
        redirectAttributes.addFlashAttribute("error", errorMessage);
        redirectAttributes.addFlashAttribute("landingPageSlogan", slogan);
        redirectAttributes.addFlashAttribute("landingPageDescription", description);
        redirectAttributes.addFlashAttribute("studioAddress", address);
    }
}
