package com.miravale.portfolio.service;

import com.miravale.portfolio.model.ArtCollection;
import com.miravale.portfolio.model.Artwork;
import com.miravale.portfolio.model.SiteSettings;
import com.miravale.portfolio.repository.ArtCollectionRepository;
import com.miravale.portfolio.repository.ArtworkRepository;
import com.miravale.portfolio.repository.SiteSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class CollectionService {
    private final ArtCollectionRepository collectionRepository;
    private final ArtworkRepository artworkRepository;
    private final SiteSettingsRepository settingsRepository;

    public CollectionService(
            ArtCollectionRepository collectionRepository,
            ArtworkRepository artworkRepository,
            SiteSettingsRepository settingsRepository) {
        this.collectionRepository = collectionRepository;
        this.artworkRepository = artworkRepository;
        this.settingsRepository = settingsRepository;
    }

    public ArtCollection findCollection(Long id) {
        return id == null ? null : collectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Collection no longer exists."));
    }

    @Transactional
    public void updateCollection(Long id, String name, String description, Long previewArtworkId) {
        ArtCollection collection = findCollection(id);
        Artwork preview = null;
        if (previewArtworkId != null) {
            preview = artworkRepository.findById(previewArtworkId)
                    .orElseThrow(() -> new IllegalArgumentException("Preview artwork no longer exists."));
            if (preview.getCollection() == null || !id.equals(preview.getCollection().getId())) {
                throw new IllegalArgumentException("The preview artwork must belong to this collection.");
            }
        }
        collection.setName(name);
        collection.setDescription(description);
        collection.setPreviewArtwork(preview);
        collectionRepository.save(collection);
    }

    @Transactional
    public void highlightCollection(Long id) {
        ArtCollection collection = findCollection(id);
        SiteSettings settings = settingsRepository.findById(SiteSettings.ID).orElseGet(SiteSettings::new);
        settings.setHighlightedCollection(collection);
        settingsRepository.save(settings);
    }

    @Transactional
    public boolean saveArtwork(Artwork artwork) {
        boolean previewCleared = false;
        if (artwork.getId() != null) {
            Long collectionId = artwork.getCollection() == null ? null : artwork.getCollection().getId();
            for (ArtCollection collection : collectionRepository.findByPreviewArtworkId(artwork.getId())) {
                if (!Objects.equals(collection.getId(), collectionId)) {
                    collection.setPreviewArtwork(null);
                    previewCleared = true;
                }
            }
        }
        artworkRepository.save(artwork);
        return previewCleared;
    }

    @Transactional
    public boolean deleteArtwork(Artwork artwork) {
        var collections = collectionRepository.findByPreviewArtworkId(artwork.getId());
        collections.forEach(collection -> collection.setPreviewArtwork(null));
        collectionRepository.flush();
        artworkRepository.delete(artwork);
        return !collections.isEmpty();
    }

    @Transactional
    public void deleteCollection(Long id) {
        ArtCollection collection = findCollection(id);
        settingsRepository.findById(SiteSettings.ID).ifPresent(settings -> {
            if (settings.getHighlightedCollection() != null
                    && id.equals(settings.getHighlightedCollection().getId())) {
                settings.setHighlightedCollection(null);
            }
        });
        collection.setPreviewArtwork(null);
        artworkRepository.findByCollectionIdOrderByCreatedDateDescIdDesc(id)
                .forEach(artwork -> artwork.setCollection(null));
        artworkRepository.flush();
        collectionRepository.delete(collection);
    }
}
