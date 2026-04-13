package org.example.scraper.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "scrape_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScrapeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "vacancies_found")
    private Integer vacanciesFound;

    @Column(name = "vacancies_added")
    private Integer vacanciesAdded;

    @Column(name = "vacancies_updated")
    private Integer vacanciesUpdated;

    @Column(name = "vacancies_deactivated")
    private Integer vacanciesDeactivated;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ScrapeStatus status = ScrapeStatus.RUNNING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public enum ScrapeStatus {
        RUNNING, SUCCESS, FAILED
    }
}