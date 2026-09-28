package com.miravale.portfolio.controller;

import com.miravale.portfolio.repository.ArtworkRepository;
import com.miravale.portfolio.repository.ExhibitionRepository;
import com.miravale.portfolio.model.SiteSettings;
import com.miravale.portfolio.repository.SiteSettingsRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
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
        SiteSettings settings = siteSettingsRepository.findById(SiteSettings.ID).orElseGet(SiteSettings::new);
        model.addAttribute("heroImageFilename", settings.getHeroImageFilename());
        String slogan = valueOrDefault(
                settings.getLandingPageSlogan(),
                SiteSettings.DEFAULT_LANDING_PAGE_SLOGAN);
        addSlogan(model, slogan);
        model.addAttribute(
                "landingPageDescription",
                valueOrDefault(
                        settings.getLandingPageDescription(),
                        SiteSettings.DEFAULT_LANDING_PAGE_DESCRIPTION));
        model.addAttribute(
                "studioAddress",
                valueOrDefault(settings.getStudioAddress(), SiteSettings.DEFAULT_STUDIO_ADDRESS));
        return "portfolio";
    }

    @GetMapping("/login")
    String login() {
        return "login";
    }

    private String valueOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private void addSlogan(Model model, String slogan) {
        int lastWhitespace = -1;
        for (int index = slogan.length() - 1; index >= 0; index--) {
            if (Character.isWhitespace(slogan.charAt(index))) {
                lastWhitespace = index;
                break;
            }
        }

        if (lastWhitespace < 0) {
            model.addAttribute("landingPageSloganLead", "");
            model.addAttribute("landingPageSloganEmphasis", slogan);
            model.addAttribute("sloganEmphasisOnNewLine", false);
            model.addAttribute("sloganHasLead", false);
            return;
        }

        model.addAttribute("landingPageSloganLead", slogan.substring(0, lastWhitespace).stripTrailing());
        model.addAttribute("landingPageSloganEmphasis", slogan.substring(lastWhitespace + 1).stripLeading());
        model.addAttribute("sloganEmphasisOnNewLine", slogan.charAt(lastWhitespace) == '\n');
        model.addAttribute("sloganHasLead", true);
    }
}
