package org.example.scraper.integration.specification;

import org.example.scraper.dto.JobFilterRequest;
import org.example.scraper.entity.Job;
import org.example.scraper.repository.JobRepository;
import org.example.scraper.repository.JobSpecification;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JobSpecificationIntegrationTest {

    @Autowired
    private JobRepository jobRepository;

    private final PageRequest PAGE = PageRequest.of(0, 20);

    @BeforeEach
    void seed() {
        jobRepository.deleteAll();
        jobRepository.saveAll(List.of(
                job("1", "Backend Engineer",   "Techstars", "Remote, USA",  "Senior",    "Software", "Full-time", true,  true),
                job("2", "Frontend Developer", "Google",    "New York, NY", "Junior",    "SaaS",     "Full-time", false, true),
                job("3", "Data Scientist",     "OpenAI",    "Remote",       "Senior",    "AI",       "Contract",  true,  true),
                job("4", "Product Manager",    "Techstars", "Kyiv, UA",     "Mid-level", "Fintech",  "Full-time", false, true),
                job("5", "DevOps Engineer",    "Amazon",    "Remote",       "Senior",    "Cloud",    "Full-time", true,  false)
        ));
    }

    private Job job(String extId, String title, String company, String location,
                    String jobFunction, String industry, String empType,
                    boolean remote, boolean active) {
        return Job.builder()
                .externalId(extId).title(title).companyName(company)
                .location(location).jobFunction(jobFunction).industry(industry)
                .employmentType(empType).remote(remote).active(active)
                .tags(List.of()).build();
    }

    private Page<Job> find(JobFilterRequest f) {
        return jobRepository.findAll(new JobSpecification(f), PAGE);
    }

    @Test
    @DisplayName("returns only active jobs when filter is empty")
    void returnsAllActiveForEmptyFilter() {
        assertThat(find(new JobFilterRequest()).getTotalElements()).isEqualTo(4);
    }

    @Nested
    @DisplayName("title filter")
    class TitleFilter {

        @Test
        @DisplayName("filters by partial title case-insensitive (only active)")
        void filtersByPartialTitle() {
            JobFilterRequest f = new JobFilterRequest();
            f.setTitle("engineer");
            assertThat(find(f).getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("returns empty when title has no match")
        void returnsEmptyForNoMatch() {
            JobFilterRequest f = new JobFilterRequest();
            f.setTitle("Astronaut");
            assertThat(find(f).getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("companyName filter")
    class CompanyFilter {

        @Test
        @DisplayName("filters by company name among active jobs")
        void filtersByCompany() {
            JobFilterRequest f = new JobFilterRequest();
            f.setCompanyName("techstars");
            assertThat(find(f).getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("returns empty when company has no active jobs")
        void returnsEmptyForInactiveCompany() {
            JobFilterRequest f = new JobFilterRequest();
            f.setCompanyName("Amazon");
            assertThat(find(f).getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("location filter")
    class LocationFilter {

        @Test
        @DisplayName("filters by partial location among active jobs")
        void filtersByLocation() {
            JobFilterRequest f = new JobFilterRequest();
            f.setLocation("Remote");
            assertThat(find(f).getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("filters by city name")
        void filtersByCity() {
            JobFilterRequest f = new JobFilterRequest();
            f.setLocation("Kyiv");
            assertThat(find(f).getTotalElements()).isEqualTo(1);
            assertThat(find(f).getContent().get(0).getExternalId()).isEqualTo("4");
        }
    }

    @Nested
    @DisplayName("remote filter")
    class RemoteFilter {

        @Test
        @DisplayName("returns only active remote jobs")
        void returnsRemoteJobs() {
            JobFilterRequest f = new JobFilterRequest();
            f.setRemote(true);
            Page<Job> result = find(f);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).allMatch(Job::getRemote);
        }

        @Test
        @DisplayName("returns only active non-remote jobs")
        void returnsNonRemoteJobs() {
            JobFilterRequest f = new JobFilterRequest();
            f.setRemote(false);
            Page<Job> result = find(f);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).allMatch(j -> !j.getRemote());
        }
    }

    @Nested
    @DisplayName("combined filters")
    class CombinedFilters {

        @Test
        @DisplayName("applies multiple filters with AND logic")
        void appliesMultipleFilters() {
            JobFilterRequest f = new JobFilterRequest();
            f.setCompanyName("Techstars");
            f.setRemote(true);
            Page<Job> result = find(f);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getExternalId()).isEqualTo("1");
        }

        @Test
        @DisplayName("returns empty when combined filters match nothing")
        void returnsEmptyWhenNothingMatches() {
            JobFilterRequest f = new JobFilterRequest();
            f.setCompanyName("Google");
            f.setRemote(true);
            assertThat(find(f).getTotalElements()).isZero();
        }

        @Test
        @DisplayName("title + jobFunction combination works")
        void titleAndJobFunctionCombination() {
            JobFilterRequest f = new JobFilterRequest();
            f.setTitle("Data");
            f.setJobFunction("Senior");
            Page<Job> result = find(f);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Data Scientist");
        }
    }
}