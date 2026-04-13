package org.example.scraper.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.scraper.service.ScraperService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScraperScheduler {

    private final ScraperService scraperService;

    @Scheduled(cron = "${scraper.cron}")
    public void scheduledScrape() {
        log.info("Scheduled scrape started");
        scraperService.scrape();
        log.info("Scheduled scrape finished");
    }

    @Scheduled(initialDelay = 5000, fixedDelay = Long.MAX_VALUE)
    public void scrapeOnStartup() {
        log.info("Initial scrape on startup");
        scraperService.scrape();
    }
}