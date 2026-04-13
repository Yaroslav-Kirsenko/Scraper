# Techstars Job Scraper

A Spring Boot application that scrapes job listings from [jobs.techstars.com](https://jobs.techstars.com/jobs) and provides a REST API to query them with filters and pagination.

## Features

- Automatically scrapes job listings using Selenium (Chrome headless)
- Stores jobs in PostgreSQL
- REST API with filtering by title, company, location, tags, remote, industry, job function
- Pagination support
- Scrape logs (start time, end time, vacancies found/added/updated/deactivated)
- Swagger UI for API exploration
- Scheduled scraping every 6 hours
- Manual scrape trigger via API

## Tech Stack

- Java 21
- Spring Boot 3
- PostgreSQL
- Selenium + WebDriverManager
- Jsoup
- Swagger / SpringDoc
- Docker + Docker Compose
- Maven

## API Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/api/jobs` | List jobs with filters and pagination |
| GET | `/api/jobs/{id}` | Get job by ID |
| POST | `/api/scraper/trigger` | Trigger scrape manually |
| GET | `/api/scraper/logs` | View scrape logs |

## Swagger UI

After starting the app, open: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## Filter Parameters

| Parameter | Type | Example |
|-----------|------|---------|
| `title` | String | `Backend Engineer` |
| `companyName` | String | `Google` |
| `location` | String | `New York` |
| `jobFunction` | String | `Engineering` |
| `industry` | String | `Software` |
| `employmentType` | String | `Full-time` |
| `remote` | Boolean | `true` |
| `tag` | String | `Machine Learning` |

See [INSTALL.md](INSTALL.md) for setup instructions.