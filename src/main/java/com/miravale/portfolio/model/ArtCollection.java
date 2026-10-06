package com.miravale.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
public class ArtCollection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 150)
    @Column(length = 150)
    private String name;

    @NotBlank
    @Size(max = 2000)
    @Column(length = 2000)
    private String description;

    @ManyToOne
    private Artwork previewArtwork;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Artwork getPreviewArtwork() { return previewArtwork; }
    public void setPreviewArtwork(Artwork previewArtwork) { this.previewArtwork = previewArtwork; }
}
