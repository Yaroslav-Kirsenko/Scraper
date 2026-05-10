package org.example.scraper.integration.repository;

import org.example.scraper.entity.ScrapeLog;
import org.example.scraper.repository.ScrapeLogRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ScrapeLogRepositoryIntegrationTest {

    @Autowired
    private ScrapeLogRepository scrapeLogRepository;

    @BeforeEach
    void cleanUp() {
        scrapeLogRepository.deleteAll();
    }

    private ScrapeLog buildLog(LocalDateTime startedAt, ScrapeLog.ScrapeStatus status,
                               int found, int added, int updated, int deactivated) {
        return ScrapeLog.builder()
                .startedAt(startedAt)
                .finishedAt(startedAt.plusMinutes(2))
                .status(status)
                .vacanciesFound(found)
                .vacanciesAdded(added)
                .vacanciesUpdated(updated)
                .vacanciesDeactivated(deactivated)
                .build();
    }

    @Nested
    @DisplayName("findAllByOrderByStartedAtDesc()")
    class FindAllByOrderByStartedAtDesc {

        @Test
        @DisplayName("returns logs ordered by startedAt descending")
        void returnsLogsOrderedDesc() {
            LocalDateTime base = LocalDateTime.of(2024, 5, 1, 10, 0);
            scrapeLogRepository.saveAll(List.of(
                    buildLog(base,              ScrapeLog.ScrapeStatus.SUCCESS, 10, 2, 8, 0),
                    buildLog(base.plusHours(1), ScrapeLog.ScrapeStatus.FAILED,   0, 0, 0, 0),
                    buildLog(base.plusHours(2), ScrapeLog.ScrapeStatus.SUCCESS, 20, 5, 15, 1)
            ));

            Page<ScrapeLog> page = scrapeLogRepository
                    .findAllByOrderByStartedAtDesc(PageRequest.of(0, 10));

            assertThat(page.getTotalElements()).isEqualTo(3);
            List<ScrapeLog> content = page.getContent();
            assertThat(content.get(0).getStartedAt()).isEqualTo(base.plusHours(2));
            assertThat(content.get(1).getStartedAt()).isEqualTo(base.plusHours(1));
            assertThat(content.get(2).getStartedAt()).isEqualTo(base);
        }

        @Test
        @DisplayName("respects page size")
        void respectsPageSize() {
            LocalDateTime base = LocalDateTime.of(2024, 5, 1, 10, 0);
            for (int i = 0; i < 5; i++) {
                scrapeLogRepository.save(
                        buildLog(base.plusHours(i), ScrapeLog.ScrapeStatus.SUCCESS, 5, 1, 4, 0));
            }

            Page<ScrapeLog> page = scrapeLogRepository
                    .findAllByOrderByStartedAtDesc(PageRequest.of(0, 2));

            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getTotalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("returns empty page when no logs exist")
        void returnsEmptyPage() {
            Page<ScrapeLog> page = scrapeLogRepository
                    .findAllByOrderByStartedAtDesc(PageRequest.of(0, 10));
            assertThat(page.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("ScrapeStatus enum persistence")
    class StatusPersistence {

        @Test
        @DisplayName("persists FAILED status with error message")
        void persistsFailedStatusWithErrorMessage() {
            ScrapeLog log = ScrapeLog.builder()
                    .startedAt(LocalDateTime.now())
                    .status(ScrapeLog.ScrapeStatus.FAILED)
                    .errorMessage("Connection timeout after 30s")
                    .build();
            ScrapeLog saved = scrapeLogRepository.save(log);

            ScrapeLog loaded = scrapeLogRepository.findById(saved.getId()).get();
            assertThat(loaded.getStatus()).isEqualTo(ScrapeLog.ScrapeStatus.FAILED);
            assertThat(loaded.getErrorMessage()).isEqualTo("Connection timeout after 30s");
        }

        @Test
        @DisplayName("persists all numeric fields correctly")
        void persistsNumericFields() {
            ScrapeLog log = buildLog(LocalDateTime.now(),
                    ScrapeLog.ScrapeStatus.SUCCESS, 42, 10, 30, 2);
            ScrapeLog saved = scrapeLogRepository.save(log);

            ScrapeLog loaded = scrapeLogRepository.findById(saved.getId()).get();
            assertThat(loaded.getVacanciesFound()).isEqualTo(42);
            assertThat(loaded.getVacanciesAdded()).isEqualTo(10);
            assertThat(loaded.getVacanciesUpdated()).isEqualTo(30);
            assertThat(loaded.getVacanciesDeactivated()).isEqualTo(2);
        }
    }
}