package org.example.scraper.integration.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.example.scraper.dto.JobFilterRequest;
import org.example.scraper.entity.Job;
import org.example.scraper.repository.JobRepository;
import org.example.scraper.service.JobService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JobServiceIntegrationTest {

    @Autowired
    private JobService jobService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private EntityManager entityManager;

    private Job buildJob(String externalId, String title) {
        return Job.builder()
                .externalId(externalId)
                .title(title)
                .companyName("Techstars")
                .location("Remote")
                .jobUrl("https://jobs.techstars.com/jobs/" + externalId)
                .tags(new ArrayList<>(List.of("Senior", "Engineering")))
                .jobFunction("Senior")
                .industry("Software")
                .remote(true)
                .active(true)
                .build();
    }

    @BeforeEach
    void cleanUp() {
        jobRepository.deleteAll();
    }

    @Nested
    @DisplayName("upsert() — real DB")
    class Upsert {

        @Test
        @DisplayName("saves new job and returns true")
        void savesNewJob() {
            boolean isNew = jobService.upsert(buildJob("ext-1", "Backend Engineer"));

            assertThat(isNew).isTrue();
            assertThat(jobRepository.existsByExternalId("ext-1")).isTrue();
        }

        @Test
        @DisplayName("updates existing job and returns false")
        void updatesExistingJob() {
            jobRepository.save(buildJob("ext-1", "Old Title"));
            entityManager.flush();
            entityManager.clear();

            Job updated = buildJob("ext-1", "New Title");
            updated.setCompanyName("New Company");

            boolean isNew = jobService.upsert(updated);

            entityManager.flush();
            entityManager.clear();

            assertThat(isNew).isFalse();
            Job inDb = jobRepository.findByExternalId("ext-1").get();
            assertThat(inDb.getTitle()).isEqualTo("New Title");
            assertThat(inDb.getCompanyName()).isEqualTo("New Company");
        }

        @Test
        @DisplayName("re-activates a deactivated job on upsert")
        void reactivatesDeactivatedJob() {
            Job inactive = buildJob("ext-1", "Dev");
            inactive.setActive(false);
            jobRepository.save(inactive);
            entityManager.flush();
            entityManager.clear();

            jobService.upsert(buildJob("ext-1", "Dev"));
            entityManager.flush();
            entityManager.clear();

            assertThat(jobRepository.findByExternalId("ext-1").get().getActive()).isTrue();
        }

        @Test
        @DisplayName("saves tags to job_tags table")
        void savesTags() {
            Job job = buildJob("ext-1", "ML Engineer");
            job.setTags(new ArrayList<>(List.of("Senior", "Machine Learning", "Python")));

            jobService.upsert(job);
            entityManager.flush();
            entityManager.clear();

            Job saved = jobRepository.findByExternalId("ext-1").get();
            assertThat(saved.getTags())
                    .containsExactlyInAnyOrder("Senior", "Machine Learning", "Python");
        }
    }

    @Nested
    @DisplayName("findById() — real DB")
    class FindById {

        @Test
        @DisplayName("returns job by id from DB")
        void returnsJobById() {
            Job saved = jobRepository.save(buildJob("ext-1", "QA Engineer"));
            entityManager.flush();
            entityManager.clear();

            Job found = jobService.findById(saved.getId());

            assertThat(found.getExternalId()).isEqualTo("ext-1");
            assertThat(found.getTitle()).isEqualTo("QA Engineer");
        }

        @Test
        @DisplayName("throws EntityNotFoundException for unknown id")
        void throwsForUnknownId() {
            assertThatThrownBy(() -> jobService.findById(999999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("999999");
        }
    }

    @Nested
    @DisplayName("findAll() with filter — real DB")
    class FindAll {

        @BeforeEach
        void seedJobs() {
            jobRepository.saveAll(List.of(
                    buildJob("ext-1", "Backend Engineer"),
                    buildJob("ext-2", "Frontend Developer"),
                    buildJob("ext-3", "Data Scientist")
            ));
            entityManager.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("returns all jobs for empty filter")
        void returnsAllForEmptyFilter() {
            Page<Job> result = jobService.findAll(new JobFilterRequest(), PageRequest.of(0, 20));
            assertThat(result.getTotalElements()).isEqualTo(3);
        }

        @Test
        @DisplayName("filters by title")
        void filtersByTitle() {
            JobFilterRequest filter = new JobFilterRequest();
            filter.setTitle("engineer");

            Page<Job> result = jobService.findAll(filter, PageRequest.of(0, 20));

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Backend Engineer");
        }

        @Test
        @DisplayName("returns empty page when filter matches nothing")
        void returnsEmptyPageForNoMatch() {
            JobFilterRequest filter = new JobFilterRequest();
            filter.setTitle("Astronaut");

            assertThat(jobService.findAll(filter, PageRequest.of(0, 20)).getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("deactivateMissing() — real DB")
    class DeactivateMissing {

        @Test
        @DisplayName("deactivates jobs absent from current scrape")
        void deactivatesMissingJobs() {
            jobRepository.saveAll(List.of(
                    buildJob("ext-1", "Job 1"),
                    buildJob("ext-2", "Job 2"),
                    buildJob("ext-3", "Job 3")
            ));
            entityManager.flush();
            entityManager.clear();

            int count = jobService.deactivateMissing(List.of("ext-1", "ext-2"));
            entityManager.clear();

            assertThat(count).isEqualTo(1);
            assertThat(jobRepository.findByExternalId("ext-3").get().getActive()).isFalse();
            assertThat(jobRepository.findByExternalId("ext-1").get().getActive()).isTrue();
            assertThat(jobRepository.findByExternalId("ext-2").get().getActive()).isTrue();
        }

        @Test
        @DisplayName("returns 0 when all active jobs are still present")
        void returnsZeroWhenNothingToDeactivate() {
            jobRepository.saveAll(List.of(
                    buildJob("ext-1", "Job 1"),
                    buildJob("ext-2", "Job 2")
            ));
            entityManager.flush();
            entityManager.clear();

            assertThat(jobService.deactivateMissing(List.of("ext-1", "ext-2"))).isZero();
        }

        @Test
        @DisplayName("full scrape cycle: upsert new + deactivate missing")
        void fullScrapeCycle() {
            jobRepository.saveAll(List.of(
                    buildJob("ext-1", "Job 1"),
                    buildJob("ext-2", "Job 2"),
                    buildJob("ext-3", "Job 3")
            ));
            entityManager.flush();
            entityManager.clear();

            jobService.upsert(buildJob("ext-1", "Job 1 Updated"));
            jobService.upsert(buildJob("ext-4", "Job 4 New"));
            entityManager.flush();
            entityManager.clear();

            int deactivated = jobService.deactivateMissing(List.of("ext-1", "ext-4"));
            entityManager.clear();

            assertThat(deactivated).isEqualTo(2);
            assertThat(jobRepository.findByExternalId("ext-1").get().getActive()).isTrue();
            assertThat(jobRepository.findByExternalId("ext-4").get().getActive()).isTrue();
            assertThat(jobRepository.findByExternalId("ext-2").get().getActive()).isFalse();
            assertThat(jobRepository.findByExternalId("ext-3").get().getActive()).isFalse();
            assertThat(jobRepository.findAllActiveExternalIds())
                    .containsExactlyInAnyOrder("ext-1", "ext-4");
        }
    }
}