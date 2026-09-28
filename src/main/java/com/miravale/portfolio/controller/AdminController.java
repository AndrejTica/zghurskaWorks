package com.miravale.portfolio.controller;

import com.miravale.portfolio.model.Artwork;
import com.miravale.portfolio.model.Exhibition;
import com.miravale.portfolio.repository.ArtworkRepository;
import com.miravale.portfolio.repository.ExhibitionRepository;
import com.miravale.portfolio.service.ImageStorageService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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
    private final ImageStorageService imageStorageService;

    public AdminController(
            ArtworkRepository artworkRepository,
            ExhibitionRepository exhibitionRepository,
            ImageStorageService imageStorageService) {
        this.artworkRepository = artworkRepository;
        this.exhibitionRepository = exhibitionRepository;
        this.imageStorageService = imageStorageService;
    }

    @GetMapping("/admin")
    String admin(Model model) {
        if (!model.containsAttribute("artwork")) {
            model.addAttribute("artwork", new Artwork());
        }
        if (!model.containsAttribute("exhibition")) {
            Exhibition exhibition = new Exhibition();
            exhibition.setDifficulty(1);
            model.addAttribute("exhibition", exhibition);
        }
        addLists(model);
        return "admin";
    }

    @PostMapping("/admin/artworks")
    String createArtwork(
            @Valid @ModelAttribute Artwork artwork,
            BindingResult bindingResult,
            @RequestParam("image") MultipartFile image,
            Model model,
            RedirectAttributes redirectAttributes) {
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
            artworkRepository.save(artwork);
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

    @PostMapping("/admin/artworks/{id}/delete")
    String deleteArtwork(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Artwork artwork = artworkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Artwork not found."));
        artworkRepository.delete(artwork);
        imageStorageService.delete(artwork.getImageFilename());
        redirectAttributes.addFlashAttribute("success", "Artwork deleted.");
        return "redirect:/admin";
    }

    @PostMapping("/admin/exhibitions/{id}/delete")
    String deleteExhibition(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (!exhibitionRepository.existsById(id)) {
            throw new IllegalArgumentException("Exhibition not found.");
        }
        exhibitionRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Exhibition deleted.");
        return "redirect:/admin";
    }

    private void addLists(Model model) {
        model.addAttribute("artworks", artworkRepository.findAll());
        model.addAttribute("exhibitions", exhibitionRepository.findAllByOrderByStartDateAsc());
    }
}
