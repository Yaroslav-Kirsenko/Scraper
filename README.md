# Techstars Job Scraper

A Spring Boot application that scrapes job listings from https://jobs.techstars.com/jobs and provides a REST API to query them with filters and pagination.

---

## Features

* Automated job scraping using Selenium (Chrome headless)
* Stores data in PostgreSQL
* REST API with filtering and pagination
* Scheduled scraping every 6 hours
* Manual scraping trigger via API
* Scrape logs tracking (start/end time, stats)
* Google Sheets integration for logging results
* Swagger UI for API testing

---

## Tech Stack

* Java 21
* Spring Boot 3
* PostgreSQL
* Selenium + WebDriverManager
* Jsoup
* Spring Data JPA
* Swagger (SpringDoc)
* Docker & Docker Compose
* Maven

---

## API Endpoints

| Method | Endpoint               | Description                        |
| ------ | ---------------------- | ---------------------------------- |
| GET    | `/api/jobs`            | Get jobs with filters & pagination |
| GET    | `/api/jobs/{id}`       | Get job by ID                      |
| POST   | `/api/scraper/trigger` | Run scraper manually               |
| GET    | `/api/scraper/logs`    | Get scraping logs                  |

---

## Filters

| Parameter      | Example          |
| -------------- | ---------------- |
| title          | Backend Engineer |
| companyName    | Google           |
| location       | Remote           |
| jobFunction    | Engineering      |
| industry       | Software         |
| employmentType | Full-time        |
| remote         | true             |
| tag            | Machine Learning |

---

## Google Sheets Integration

The application can log scraping results to Google Sheets.

### Features:

* Logs each scraping run
* Stores number of jobs scraped
* Tracks execution time

###️ Important

Credentials file is NOT included in the repository.

You must provide your own:

```
src/main/resources/google-credentials.json
```

---

## Swagger UI

After запуск:

http://localhost:8080/swagger-ui.html

---

## How to Run

See detailed setup instructions in:
`INSTALL.md`

---

## Project Structure

```
src/main/java/org/example/scraper
│
├── config          # Configuration classes
├── controller      # REST controllers
├── dto             # Request/response models
├── entity          # JPA entities
├── repository      # Database repositories
├── scheduler       # Scheduled tasks
├── selectors       # Scraper selectors & scripts
├── service         # Business logic (scraping, sheets, etc.)
```

---

##  Notes

* Google credentials are excluded via `.gitignore`
* Chrome browser must be installed
* First run may take time due to WebDriver download

---

## Future Improvements

* Proxy support (avoid blocking)
* User-Agent rotation
* Caching layer (Redis)
* UI dashboard

---
