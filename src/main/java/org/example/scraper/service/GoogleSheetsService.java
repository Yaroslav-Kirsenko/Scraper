package org.example.scraper.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.scraper.config.ScraperProperties;
import org.example.scraper.entity.ScrapeLog;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleSheetsService {

    private final ScraperProperties props;

    private Sheets sheetsClient;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PostConstruct
    public void init() {
        try {
            InputStream credentialsStream = getClass()
                    .getClassLoader()
                    .getResourceAsStream("google-credentials.json");

            if (credentialsStream == null) {
                log.warn("google-credentials.json not found, Google Sheets disabled");
                return;
            }

            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(credentialsStream)
                    .createScoped(List.of(SheetsScopes.SPREADSHEETS));

            sheetsClient = new Sheets.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials)
            )
                    .setApplicationName("Scraper")
                    .build();

            log.info("Google Sheets client initialized");

        } catch (Exception e) {
            log.error("Failed to initialize Google Sheets client: {}", e.getMessage());
        }
    }

    public void appendScrapeLog(ScrapeLog scrapeLog) {
        if (sheetsClient == null) {
            log.warn("Google Sheets client not initialized, skipping log");
            return;
        }

        try {
            List<Object> row = List.of(
                    scrapeLog.getStartedAt() != null
                            ? scrapeLog.getStartedAt().format(FORMATTER) : "",
                    scrapeLog.getFinishedAt() != null
                            ? scrapeLog.getFinishedAt().format(FORMATTER) : "",
                    scrapeLog.getStatus() != null
                            ? scrapeLog.getStatus().name() : "",
                    scrapeLog.getVacanciesFound() != null
                            ? scrapeLog.getVacanciesFound() : 0,
                    scrapeLog.getVacanciesAdded() != null
                            ? scrapeLog.getVacanciesAdded() : 0,
                    scrapeLog.getVacanciesUpdated() != null
                            ? scrapeLog.getVacanciesUpdated() : 0,
                    scrapeLog.getVacanciesDeactivated() != null
                            ? scrapeLog.getVacanciesDeactivated() : 0,
                    scrapeLog.getErrorMessage() != null
                            ? scrapeLog.getErrorMessage() : ""
            );

            ValueRange body = new ValueRange()
                    .setValues(List.of(row));

            sheetsClient.spreadsheets().values()
                    .append(props.getGoogleSheetsId(), "Лист1!A1", body)
                    .setValueInputOption("USER_ENTERED")
                    .execute();

            log.info("Scrape log appended to Google Sheets");

        } catch (Exception e) {
            log.error("Failed to append log to Google Sheets: {}", e.getMessage());
        }
    }
}