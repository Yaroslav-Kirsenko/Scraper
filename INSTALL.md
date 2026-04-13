# Installation Guide

## Prerequisites

### 1. Install Docker Desktop (Windows)

1. Go to [https://www.docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop)
2. Click **"Download for Windows"**
3. Run the installer and follow the steps
4. After install — **restart your computer**
5. Launch **Docker Desktop** from the Start menu
6. Wait until you see **"Docker is running"** in the system tray

> Make sure WSL 2 is enabled — Docker Desktop will prompt you if it's not.
> Install it with: `wsl --install` in PowerShell (run as Administrator)

### 2. Install Git (if not installed)

Download from [https://git-scm.com/download/win](https://git-scm.com/download/win) and install with default settings.

---

## Running with Docker (Recommended)

### Step 1 — Clone the repository

Open **PowerShell** or **Command Prompt** and run:

```bash
git clone https://github.com/Yaroslav-Kirsenko/Scraper
cd YOUR_REPO
```

### Step 2 — Start the application

```bash
docker-compose up --build
```

This command will:
- Build the Spring Boot app into a Docker image
- Start PostgreSQL database
- Start the application
- The scraper will run automatically on startup

> First run takes 3–5 minutes (downloading images and building the project)

### Step 3 — Open Swagger UI

Go to: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### Stop the application

```bash
docker-compose down
```

To also delete the database data:

```bash
docker-compose down -v
```

---

## Running without Docker (Manual Setup)

### Requirements

- Java 21+ — [https://adoptium.net](https://adoptium.net)
- Maven 3.9+ — [https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi)
- PostgreSQL 16 — [https://www.postgresql.org/download/windows](https://www.postgresql.org/download/windows)
- Google Chrome browser (latest version)

### Step 1 — Create the database

Open pgAdmin or psql and run:

```sql
CREATE DATABASE scraper;
```

### Step 2 — Configure the application

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/scraper
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD
```

### Step 3 — Build and run

```bash
mvn clean package -DskipTests
java -jar target/*.jar
```

### Step 4 — Open Swagger UI

Go to: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## Scraper Configuration

You can adjust scraper behavior in `application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `scraper.cron` | `0 0 */6 * * *` | Scrape schedule (every 6 hours) |
| `scraper.max-scrape-duration-ms` | `60000` | Max scroll time in ms (60 seconds) |
| `scraper.scroll-delay-ms` | `1500` | Delay after each scroll |
| `scraper.loading-delay-ms` | `2000` | Delay after clicking Load More |
| `scraper.timeout-seconds` | `30` | WebDriver wait timeout |