package org.example.scraper.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.scraper.config.ScraperProperties;
import org.example.scraper.entity.Job;
import org.example.scraper.entity.ScrapeLog;
import org.example.scraper.repository.ScrapeLogRepository;
import org.example.scraper.selectors.BrowserScripts;
import org.example.scraper.selectors.TechstarsSelectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScraperService {

    private final ScraperProperties props;
    private final JobService jobService;
    private final ScrapeLogRepository scrapeLogRepository;


    public void scrape() {
        ScrapeLog scrapeLog = ScrapeLog.builder()
                .startedAt(LocalDateTime.now())
                .status(ScrapeLog.ScrapeStatus.RUNNING)
                .build();
        scrapeLogRepository.save(scrapeLog);

        int found = 0, added = 0, updated = 0, deactivated = 0;

        try {
            List<Job> scrapedJobs = scrapeAllJobs();
            found = scrapedJobs.size();
            log.info("Scraped {} jobs total", found);

            List<String> scrapedExternalIds = new ArrayList<>();
            for (Job job : scrapedJobs) {
                scrapedExternalIds.add(job.getExternalId());
                boolean isNew = jobService.upsert(job);
                if (isNew) added++;
                else updated++;
            }

            deactivated = jobService.deactivateMissing(scrapedExternalIds);
            scrapeLog.setStatus(ScrapeLog.ScrapeStatus.SUCCESS);

        } catch (Exception e) {
            log.error("Scraping failed: {}", e.getMessage(), e);
            scrapeLog.setStatus(ScrapeLog.ScrapeStatus.FAILED);
            scrapeLog.setErrorMessage(e.getMessage());

        } finally {
            scrapeLog.setFinishedAt(LocalDateTime.now());
            scrapeLog.setVacanciesFound(found);
            scrapeLog.setVacanciesAdded(added);
            scrapeLog.setVacanciesUpdated(updated);
            scrapeLog.setVacanciesDeactivated(deactivated);
            scrapeLogRepository.save(scrapeLog);
        }
    }

    private WebDriver createDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-agent=" + props.getUserAgent());
        return new ChromeDriver(options);
    }

    private void loadAllJobs(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector(TechstarsSelectors.JOB_CARD)));

        log.info("Page loaded, starting to scroll...");
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < props.getMaxScrapeDurationMs()) {
            int countBefore = driver.findElements(
                    By.cssSelector(TechstarsSelectors.JOB_CARD)).size();

            log.info("Jobs loaded so far: {}", countBefore);

            ((JavascriptExecutor) driver).executeScript(BrowserScripts.SCROLL_TO_BOTTOM);
            Thread.sleep(props.getScrollDelayMs());

            clickLoadMoreIfPresent(driver);

            int countAfter = driver.findElements(
                    By.cssSelector(TechstarsSelectors.JOB_CARD)).size();

            if (countAfter == countBefore) {
                log.info("No new jobs loaded, finishing early. Total: {}", countAfter);
                break;
            }
        }
    }

    private void clickLoadMoreIfPresent(WebDriver driver) throws InterruptedException {
        List<WebElement> buttons = driver.findElements(
                By.cssSelector(TechstarsSelectors.LOAD_MORE_BTN));

        if (buttons.isEmpty()) return;

        WebElement loadMore = buttons.get(0);
        if (!loadMore.isDisplayed() || !loadMore.isEnabled()
                || "true".equals(loadMore.getAttribute("data-loading"))) return;

        ((JavascriptExecutor) driver).executeScript(BrowserScripts.SCROLL_INTO_VIEW, loadMore);
        Thread.sleep(props.getButtonDelayMs());

        try {
            loadMore.click();
        } catch (Exception e) {
            ((JavascriptExecutor) driver).executeScript(BrowserScripts.CLICK, loadMore);
        }

        log.info("Clicked 'Load more'");
        Thread.sleep(props.getLoadingDelayMs());
    }

    private List<Job> scrapeAllJobs() {
        WebDriver driver = createDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(props.getTimeoutSeconds()));
        List<Job> result = new ArrayList<>();

        try {
            driver.get(props.getUrl());
            loadAllJobs(driver, wait);

            Document doc = Jsoup.parse(driver.getPageSource());
            Elements jobCards = doc.select(TechstarsSelectors.JOB_CARD);
            log.info("Parsing {} job cards from final HTML", jobCards.size());

            for (Element card : jobCards) {
                try {
                    Job job = parseJobCard(card);
                    if (!job.getTitle().isBlank()) result.add(job);
                } catch (Exception e) {
                    log.warn("Failed to parse card: {}", e.getMessage());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Scraping interrupted: {}", e.getMessage());
        } finally {
            driver.quit();
        }

        return result;
    }

    private Job parseJobCard(Element card) {
        String title = "";
        Element titleEl = card.selectFirst(TechstarsSelectors.JOB_TITLE);
        if (titleEl != null) title = titleEl.text().trim();

        String jobUrl = "";
        String externalId = UUID.randomUUID().toString();
        Element jobLink = card.selectFirst(TechstarsSelectors.JOB_LINK);
        if (jobLink != null) {
            String href = jobLink.attr("href").replaceAll("#.*$", "");
            jobUrl = href.startsWith("http") ? href : props.getBaseUrl() + href;
            externalId = extractJobId(href);
        }

        String companyName = "";
        Element companyLink = card.selectFirst(TechstarsSelectors.COMPANY_LINK);
        if (companyLink != null) companyName = companyLink.text().trim();

        String location = "";
        Element locationDiv = card.selectFirst(TechstarsSelectors.LOCATION_DIV);
        if (locationDiv != null) {
            Element firstSpan = locationDiv.selectFirst("span");
            if (firstSpan != null) location = firstSpan.text().trim();
        }
        if (location.isBlank()) {
            Element metaLoc = card.selectFirst(TechstarsSelectors.META_LOCATION);
            if (metaLoc != null) location = metaLoc.attr("content").trim();
        }

        List<String> tags = new ArrayList<>();
        Elements tagEls = card.select(TechstarsSelectors.TAG);
        for (Element tag : tagEls) {
            String t = tag.text().trim();
            if (!t.isBlank()) tags.add(t);
        }

        String description = "";
        Element metaDesc = card.selectFirst(TechstarsSelectors.META_DESC);
        if (metaDesc != null) description = metaDesc.attr("content").trim();

        boolean remote = location.toLowerCase().contains("remote")
                || tags.stream().anyMatch(t -> t.toLowerCase().contains("remote"));

        String jobFunction = tags.stream().filter(this::isSeniority).findFirst().orElse("");
        String industry = tags.stream().filter(t -> !isSeniority(t)).findFirst().orElse("");

        return Job.builder()
                .externalId(externalId)
                .title(title)
                .companyName(companyName)
                .location(location)
                .jobUrl(jobUrl)
                .tags(tags)
                .description(description)
                .remote(remote)
                .jobFunction(jobFunction)
                .industry(industry)
                .active(true)
                .build();
    }

    private String extractJobId(String url) {
        if (url == null || url.isBlank()) return UUID.randomUUID().toString();
        String[] parts = url.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i].replaceAll("#.*$", "");
            if (part.matches("\\d+.*")) return part.split("-")[0];
        }
        return UUID.nameUUIDFromBytes(url.getBytes()).toString();
    }

    private boolean isSeniority(String tag) {
        String lower = tag.toLowerCase();
        return lower.equals("senior") || lower.equals("junior")
                || lower.equals("mid-senior level") || lower.equals("entry level")
                || lower.equals("associate") || lower.equals("director")
                || lower.equals("internship") || lower.contains("level");
    }

    public Page<ScrapeLog> getLogs(Pageable pageable) {
        return scrapeLogRepository.findAllByOrderByStartedAtDesc(pageable);
    }
}