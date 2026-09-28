package com.miravale.portfolio.repository;

import com.miravale.portfolio.model.Exhibition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDate;

public interface ExhibitionRepository extends JpaRepository<Exhibition, Long> {
    List<Exhibition> findAllByOrderByStartDateAsc();
    List<Exhibition> findByEndDateGreaterThanEqualOrderByStartDateAsc(LocalDate date);
}
