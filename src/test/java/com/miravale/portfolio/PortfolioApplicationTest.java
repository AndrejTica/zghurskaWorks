package com.miravale.portfolio;

import com.miravale.portfolio.model.Artwork;
import com.miravale.portfolio.model.Exhibition;
import com.miravale.portfolio.repository.ArtworkRepository;
import com.miravale.portfolio.repository.ExhibitionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PortfolioApplicationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ArtworkRepository artworkRepository;

    @Autowired
    private ExhibitionRepository exhibitionRepository;

    @Test
    void portfolioIsPublicAndAdminRedirectsToLogin() {
        Artwork artwork = new Artwork();
        artwork.setTitle("Harbour Light");
        artwork.setDescription("Private collection");
        artwork.setCreatedDate(LocalDate.of(2026, 4, 12));
        artwork.setImageFilename("harbour-light.jpg");
        artwork.setCommissioned(true);
        artworkRepository.save(artwork);

        Exhibition exhibition = new Exhibition();
        exhibition.setTitle("Material Memory");
        exhibition.setVenue("Galerie Haus");
        exhibition.setStartDate(LocalDate.now().plusMonths(2));
        exhibition.setEndDate(LocalDate.now().plusMonths(3));
        exhibition.setDetails("A group exhibition.");
        exhibition.setDifficulty(3);
        exhibitionRepository.save(exhibition);

        Exhibition pastExhibition = new Exhibition();
        pastExhibition.setTitle("Earlier Works");
        pastExhibition.setVenue("Old Town Gallery");
        pastExhibition.setStartDate(LocalDate.now().minusYears(1));
        pastExhibition.setEndDate(LocalDate.now().minusYears(1).plusMonths(1));
        pastExhibition.setDetails("A past solo exhibition.");
        pastExhibition.setDifficulty(2);
        exhibitionRepository.save(pastExhibition);

        ResponseEntity<String> portfolio = restTemplate.getForEntity("http://localhost:" + port + "/", String.class);
        ResponseEntity<String> admin = restTemplate.getForEntity("http://localhost:" + port + "/admin", String.class);

        assertThat(portfolio.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(portfolio.getBody())
                .contains("Harbour Light", "Material Memory", "Earlier Works", "Upcoming", "★★★☆☆")
                .contains("exhibition--upcoming", "exhibition--past");
        assertThat(admin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(admin.getBody()).contains("Welcome back");
    }
}
