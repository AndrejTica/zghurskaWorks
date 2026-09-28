package com.miravale.portfolio.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

@Entity
public class SiteSettings {
    public static final long ID = 1L;
    public static final String DEFAULT_LANDING_PAGE_DESCRIPTION =
            "Anna Zghurska explores memory, place, and the quiet tension between natural and constructed worlds.";

    @Id
    private Long id = ID;

    private String heroImageFilename;

    @Column(length = 2000)
    private String landingPageDescription;

    public Long getId() { return id; }
    public String getHeroImageFilename() { return heroImageFilename; }
    public void setHeroImageFilename(String heroImageFilename) { this.heroImageFilename = heroImageFilename; }
    public String getLandingPageDescription() { return landingPageDescription; }
    public void setLandingPageDescription(String landingPageDescription) {
        this.landingPageDescription = landingPageDescription;
    }
}
