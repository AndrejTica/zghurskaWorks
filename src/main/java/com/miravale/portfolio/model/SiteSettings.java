package com.miravale.portfolio.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class SiteSettings {
    public static final long ID = 1L;

    @Id
    private Long id = ID;

    private String heroImageFilename;

    public Long getId() { return id; }
    public String getHeroImageFilename() { return heroImageFilename; }
    public void setHeroImageFilename(String heroImageFilename) { this.heroImageFilename = heroImageFilename; }
}
