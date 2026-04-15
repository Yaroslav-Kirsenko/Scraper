package org.example.scraper.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "scraper")
public class ScraperProperties {

    private String url;
    private String baseUrl;
    private String cron;
    private String userAgent;
    private String googleSheetsId;

    private int timeoutSeconds = 60;
    private long scrollDelayMs = 1500;
    private long buttonDelayMs = 500;
    private long retryDelayMs = 3000;
    private long loadingDelayMs = 2000;
    private long maxScrapeDurationMs = 60000;
}
