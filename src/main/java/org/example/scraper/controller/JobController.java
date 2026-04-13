package org.example.scraper.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.scraper.dto.JobFilterRequest;
import org.example.scraper.entity.Job;
import org.example.scraper.entity.ScrapeLog;
import org.example.scraper.service.JobService;
import org.example.scraper.service.ScraperService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "API for getting jobs ")
public class JobController {

    private final JobService jobService;
    private final ScraperService scraperService;

    @GetMapping("/api/jobs")
    @Operation(summary = "Job listing with filters and pagination")
    public Page<Job> getJobs(
            @ParameterObject JobFilterRequest filter,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return jobService.findAll(filter, pageable);
    }

    @GetMapping("/api/jobs/{id}")
    @Operation(summary = "Get a job by ID")
    public ResponseEntity<Job> getJob(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.findById(id));
    }


    @PostMapping("/api/scraper/trigger")
    @Operation(summary = "Run the scraper manually")
    public ResponseEntity<String> triggerScrape() {
        new Thread(scraperService::scrape).start();
        return ResponseEntity.accepted().body("Scrape started");
    }

    @GetMapping("/api/scraper/logs")
    @Operation(summary = "Scraper launch logs")
    public Page<ScrapeLog> getScrapeLogs(
            @ParameterObject @PageableDefault(size = 10) Pageable pageable
    ) {
        return scraperService.getLogs(pageable);
    }
}