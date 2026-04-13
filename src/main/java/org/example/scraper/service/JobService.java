package org.example.scraper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.scraper.dto.JobFilterRequest;
import org.example.scraper.entity.Job;
import org.example.scraper.repository.JobRepository;
import org.example.scraper.repository.JobSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;

    @Transactional(readOnly = true)
    public Page<Job> findAll(JobFilterRequest filter, Pageable pageable) {
        return jobRepository.findAll(new JobSpecification(filter), pageable);
    }

    @Transactional(readOnly = true)
    public Job findById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Job not found with id: " + id));
    }


    @Transactional
    public boolean upsert(Job incoming) {
        Optional<Job> existing = jobRepository.findByExternalId(incoming.getExternalId());

        if (existing.isPresent()) {
            Job job = existing.get();
            job.setTitle(incoming.getTitle());
            job.setCompanyName(incoming.getCompanyName());
            job.setLocation(incoming.getLocation());
            job.setDescription(incoming.getDescription());
            job.setJobUrl(incoming.getJobUrl());
            job.setTags(incoming.getTags());
            job.setJobFunction(incoming.getJobFunction());
            job.setIndustry(incoming.getIndustry());
            job.setEmploymentType(incoming.getEmploymentType());
            job.setRemote(incoming.getRemote());
            job.setPostedAt(incoming.getPostedAt());
            job.setActive(true);
            jobRepository.save(job);
            return false;
        } else {
            jobRepository.save(incoming);
            return true;
        }
    }

    @Transactional
    public int deactivateMissing(List<String> currentExternalIds) {
        List<String> activeIds = jobRepository.findAllActiveExternalIds();

        Set<String> currentSet = new HashSet<>(currentExternalIds);
        List<String> toDeactivate = activeIds.stream()
                .filter(id -> !currentSet.contains(id))
                .toList();

        if (toDeactivate.isEmpty()) return 0;

        int count = jobRepository.deactivateByExternalIds(toDeactivate);
        log.info("Deactivated {} jobs no longer on site", count);
        return count;
    }
}
