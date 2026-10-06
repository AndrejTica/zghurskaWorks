package com.miravale.portfolio.controller;

import com.miravale.portfolio.model.ArtCollection;
import com.miravale.portfolio.repository.ArtCollectionRepository;
import com.miravale.portfolio.service.CollectionService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CollectionAdminController {
    private final ArtCollectionRepository collectionRepository;
    private final CollectionService collectionService;

    public CollectionAdminController(
            ArtCollectionRepository collectionRepository, CollectionService collectionService) {
        this.collectionRepository = collectionRepository;
        this.collectionService = collectionService;
    }

    @InitBinder("artCollection")
    void collectionFields(WebDataBinder binder) {
        binder.setAllowedFields("name", "description");
    }

    @PostMapping("/admin/collections")
    String createCollection(
            @Valid @ModelAttribute ArtCollection artCollection,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "error", "Collection was not added. Name and description are required (150 and 2000 characters maximum).");
            redirectAttributes.addFlashAttribute("artCollection", artCollection);
            return "redirect:/admin";
        }
        collectionRepository.save(artCollection);
        redirectAttributes.addFlashAttribute("success", "Collection added. Assign artworks, then choose its preview.");
        return "redirect:/admin";
    }

    @PostMapping("/admin/collections/{id}")
    String updateCollection(
            @PathVariable Long id,
            @Valid @ModelAttribute ArtCollection artCollection,
            BindingResult bindingResult,
            @RequestParam(required = false) Long previewArtworkId,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "error", "Collection was not updated. Name and description are required (150 and 2000 characters maximum).");
            return "redirect:/admin";
        }
        try {
            collectionService.updateCollection(
                    id, artCollection.getName(), artCollection.getDescription(), previewArtworkId);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/admin";
        }
        redirectAttributes.addFlashAttribute("success", "Collection updated.");
        return "redirect:/admin";
    }

    @PostMapping("/admin/highlighted-collection")
    String highlightCollection(
            @RequestParam(required = false) Long collectionId,
            RedirectAttributes redirectAttributes) {
        try {
            collectionService.highlightCollection(collectionId);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/admin";
        }
        redirectAttributes.addFlashAttribute("success", "Highlighted collection updated.");
        return "redirect:/admin";
    }

    @PostMapping("/admin/collections/{id}/delete")
    String deleteCollection(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            collectionService.deleteCollection(id);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/admin";
        }
        redirectAttributes.addFlashAttribute("success", "Collection deleted. Its artworks have been kept without a collection.");
        return "redirect:/admin";
    }
}
