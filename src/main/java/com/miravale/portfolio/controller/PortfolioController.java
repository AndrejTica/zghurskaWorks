package com.miravale.portfolio.controller;

import com.miravale.portfolio.repository.ArtworkRepository;
import com.miravale.portfolio.repository.ExhibitionRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
public class PortfolioController {
    private final ArtworkRepository artworkRepository;
    private final ExhibitionRepository exhibitionRepository;

    public PortfolioController(ArtworkRepository artworkRepository, ExhibitionRepository exhibitionRepository) {
        this.artworkRepository = artworkRepository;
        this.exhibitionRepository = exhibitionRepository;
    }

    @GetMapping("/")
    String portfolio(Model model) {
        model.addAttribute("works", artworkRepository.findByCommissionedOrderByCreatedDateDesc(false));
        model.addAttribute("commissions", artworkRepository.findByCommissionedOrderByCreatedDateDesc(true));
        model.addAttribute("exhibitions",
                exhibitionRepository.findByEndDateGreaterThanEqualOrderByStartDateAsc(LocalDate.now()));
        return "portfolio";
    }

    @GetMapping("/login")
    String login() {
        return "login";
    }
}
