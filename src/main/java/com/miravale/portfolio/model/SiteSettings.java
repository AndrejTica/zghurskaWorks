package com.miravale.portfolio.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;

@Entity
public class SiteSettings {
    public static final long ID = 1L;
    public static final String DEFAULT_LANDING_PAGE_SLOGAN = "Finding stillness\nin motion.";
    public static final String DEFAULT_LANDING_PAGE_DESCRIPTION =
            "Anna Zghurska explores memory, place, and the quiet tension between natural and constructed worlds.";
    public static final String DEFAULT_STUDIO_ADDRESS = "Studio 17, Lichtgasse 8\n1070 Vienna, Austria";

    @Id
    private Long id = ID;

    private String heroImageFilename;

    @Column(length = 2000)
    private String landingPageDescription;

    @Column(length = 300)
    private String landingPageSlogan;

    @Column(length = 500)
    private String studioAddress;

    @ManyToOne
    private ArtCollection highlightedCollection;

    public Long getId() { return id; }
    public String getHeroImageFilename() { return heroImageFilename; }
    public void setHeroImageFilename(String heroImageFilename) { this.heroImageFilename = heroImageFilename; }
    public String getLandingPageDescription() { return landingPageDescription; }
    public void setLandingPageDescription(String landingPageDescription) {
        this.landingPageDescription = landingPageDescription;
    }
    public String getLandingPageSlogan() { return landingPageSlogan; }
    public void setLandingPageSlogan(String landingPageSlogan) { this.landingPageSlogan = landingPageSlogan; }
    public String getStudioAddress() { return studioAddress; }
    public void setStudioAddress(String studioAddress) { this.studioAddress = studioAddress; }
    public ArtCollection getHighlightedCollection() { return highlightedCollection; }
    public void setHighlightedCollection(ArtCollection highlightedCollection) {
        this.highlightedCollection = highlightedCollection;
    }
}
