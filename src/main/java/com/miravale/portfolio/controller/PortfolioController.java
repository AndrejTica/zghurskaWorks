package com.miravale.portfolio.controller;

import com.miravale.portfolio.repository.ArtworkRepository;
import com.miravale.portfolio.repository.ExhibitionRepository;
import com.miravale.portfolio.model.SiteSettings;
import com.miravale.portfolio.repository.SiteSettingsRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
public class PortfolioController {
    private final ArtworkRepository artworkRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final SiteSettingsRepository siteSettingsRepository;

    public PortfolioController(
            ArtworkRepository artworkRepository,
            ExhibitionRepository exhibitionRepository,
            SiteSettingsRepository siteSettingsRepository) {
        this.artworkRepository = artworkRepository;
        this.exhibitionRepository = exhibitionRepository;
        this.siteSettingsRepository = siteSettingsRepository;
    }

    @GetMapping("/")
    String portfolio(Model model) {
        model.addAttribute("works", artworkRepository.findByCommissionedOrderByCreatedDateDesc(false));
        model.addAttribute("commissions", artworkRepository.findByCommissionedOrderByCreatedDateDesc(true));
        model.addAttribute("exhibitions",
                exhibitionRepository.findByEndDateGreaterThanEqualOrderByStartDateAsc(LocalDate.now()));
        model.addAttribute("heroImageFilename", siteSettingsRepository.findById(SiteSettings.ID)
                .map(SiteSettings::getHeroImageFilename)
                .orElse(null));
        return "portfolio";
    }

    @GetMapping("/login")
    String login() {
        return "login";
    }
}
