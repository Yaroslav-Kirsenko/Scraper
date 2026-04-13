package org.example.scraper.repository;

import org.example.scraper.entity.ScrapeLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScrapeLogRepository extends JpaRepository<ScrapeLog, Long> {

    Page<ScrapeLog> findAllByOrderByStartedAtDesc(Pageable pageable);

    Optional<ScrapeLog> findTopByOrderByStartedAtDesc();
}