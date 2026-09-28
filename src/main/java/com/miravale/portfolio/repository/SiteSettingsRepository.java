package com.miravale.portfolio.repository;

import com.miravale.portfolio.model.SiteSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteSettingsRepository extends JpaRepository<SiteSettings, Long> {
}
