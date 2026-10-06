package com.miravale.portfolio.repository;

import com.miravale.portfolio.model.ArtCollection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtCollectionRepository extends JpaRepository<ArtCollection, Long> {
    List<ArtCollection> findAllByOrderByNameAscIdAsc();
    List<ArtCollection> findByPreviewArtworkId(Long artworkId);
}
