package com.miravale.portfolio.repository;

import com.miravale.portfolio.model.Artwork;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtworkRepository extends JpaRepository<Artwork, Long> {
    List<Artwork> findByCommissionedOrderByCreatedDateDesc(boolean commissioned);
}
