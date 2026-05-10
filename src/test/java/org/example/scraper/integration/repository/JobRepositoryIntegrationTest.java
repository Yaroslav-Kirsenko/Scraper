package org.example.scraper.integration.repository;

import jakarta.persistence.EntityManager;
import org.example.scraper.entity.Job;
import org.example.scraper.repository.JobRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JobRepositoryIntegrationTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private EntityManager entityManager;

    private Job buildJob(String externalId, String title, String company,
                         String location, boolean active, boolean remote) {
        return Job.builder()
                .externalId(externalId)
                .title(title)
                .companyName(company)
                .location(location)
                .jobUrl("https://jobs.techstars.com/jobs/" + externalId)
                .tags(List.of("Senior", "Engineering"))
                .jobFunction("Senior")
                .industry("Software")
                .remote(remote)
                .active(active)
                .build();
    }

    @BeforeEach
    void cleanUp() {
        jobRepository.deleteAll();
    }

    @Nested
    @DisplayName("findByExternalId()")
    class FindByExternalId {

        @Test
        @DisplayName("returns job when externalId exists")
        void returnsJobWhenExists() {
            jobRepository.save(buildJob("ext-1", "Engineer", "Techstars", "Remote", true, true));

            Optional<Job> result = jobRepository.findByExternalId("ext-1");

            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("Engineer");
        }

        @Test
        @DisplayName("returns empty when externalId does not exist")
        void returnsEmptyWhenNotFound() {
            assertThat(jobRepository.findByExternalId("nonexistent")).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByExternalId()")
    class ExistsByExternalId {

        @Test
        @DisplayName("returns true when job exists")
        void returnsTrueWhenExists() {
            jobRepository.save(buildJob("ext-1", "Engineer", "Co", "NY", true, false));
            assertThat(jobRepository.existsByExternalId("ext-1")).isTrue();
        }

        @Test
        @DisplayName("returns false when job does not exist")
        void returnsFalseWhenNotExists() {
            assertThat(jobRepository.existsByExternalId("ghost")).isFalse();
        }
    }

    @Nested
    @DisplayName("findAllActiveExternalIds()")
    class FindAllActiveExternalIds {

        @Test
        @DisplayName("returns only active external IDs")
        void returnsOnlyActiveIds() {
            jobRepository.save(buildJob("active-1",   "Dev", "A", "Remote", true,  true));
            jobRepository.save(buildJob("active-2",   "QA",  "B", "Kyiv",   true,  false));
            jobRepository.save(buildJob("inactive-1", "PM",  "C", "London", false, false));

            List<String> ids = jobRepository.findAllActiveExternalIds();

            assertThat(ids).containsExactlyInAnyOrder("active-1", "active-2");
            assertThat(ids).doesNotContain("inactive-1");
        }

        @Test
        @DisplayName("returns empty list when no active jobs")
        void returnsEmptyWhenNoActiveJobs() {
            jobRepository.save(buildJob("inactive-1", "Dev", "A", "Remote", false, true));
            assertThat(jobRepository.findAllActiveExternalIds()).isEmpty();
        }
    }

    @Nested
    @DisplayName("deactivateByExternalIds()")
    class DeactivateByExternalIds {

        @Test
        @DisplayName("deactivates matching jobs and returns count")
        void deactivatesMatchingJobs() {
            jobRepository.save(buildJob("ext-1", "Dev", "A", "Remote", true, true));
            jobRepository.save(buildJob("ext-2", "QA",  "B", "Kyiv",   true, false));
            jobRepository.save(buildJob("ext-3", "PM",  "C", "London", true, false));

            int count = jobRepository.deactivateByExternalIds(List.of("ext-1", "ext-2"));

            // Clear first-level cache so next reads hit the DB, not stale cached entities
            entityManager.clear();

            assertThat(count).isEqualTo(2);
            assertThat(jobRepository.findByExternalId("ext-1").get().getActive()).isFalse();
            assertThat(jobRepository.findByExternalId("ext-2").get().getActive()).isFalse();
            assertThat(jobRepository.findByExternalId("ext-3").get().getActive()).isTrue();
        }

        @Test
        @DisplayName("returns 0 when no IDs match")
        void returnsZeroWhenNoMatch() {
            jobRepository.save(buildJob("ext-1", "Dev", "A", "Remote", true, true));
            assertThat(jobRepository.deactivateByExternalIds(List.of("ghost-id"))).isZero();
            assertThat(jobRepository.findByExternalId("ext-1").get().getActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("findAllByActiveTrue()")
    class FindAllByActiveTrue {

        @Test
        @DisplayName("returns only active jobs with pagination")
        void returnsOnlyActiveJobsPaginated() {
            jobRepository.save(buildJob("ext-1", "Dev", "A", "Remote", true,  true));
            jobRepository.save(buildJob("ext-2", "QA",  "B", "Kyiv",   true,  false));
            jobRepository.save(buildJob("ext-3", "PM",  "C", "London", false, false));

            Page<Job> page = jobRepository.findAllByActiveTrue(PageRequest.of(0, 10));

            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent())
                    .extracting(Job::getExternalId)
                    .containsExactlyInAnyOrder("ext-1", "ext-2");
        }

        @Test
        @DisplayName("respects page size")
        void respectsPageSize() {
            for (int i = 1; i <= 5; i++) {
                jobRepository.save(buildJob("ext-" + i, "Title " + i, "Co", "Remote", true, true));
            }
            Page<Job> page = jobRepository.findAllByActiveTrue(PageRequest.of(0, 2));
            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getTotalPages()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Tags — @ElementCollection persistence")
    class TagsPersistence {

        @Test
        @DisplayName("saves and loads tags correctly")
        void savesAndLoadsTags() {
            Job job = buildJob("ext-1", "ML Engineer", "AI Corp", "Remote", true, true);
            job.setTags(List.of("Senior", "Machine Learning", "Python"));
            jobRepository.save(job);

            Job loaded = jobRepository.findByExternalId("ext-1").get();
            assertThat(loaded.getTags())
                    .containsExactlyInAnyOrder("Senior", "Machine Learning", "Python");
        }

        @Test
        @DisplayName("saves job with empty tags list")
        void savesJobWithEmptyTags() {
            Job job = buildJob("ext-1", "Dev", "Co", "Remote", true, true);
            job.setTags(List.of());
            jobRepository.save(job);
            assertThat(jobRepository.findByExternalId("ext-1").get().getTags()).isEmpty();
        }
    }
}