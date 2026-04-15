# Installation Guide

---

## ️ Prerequisites

* Java 21+
* Maven 3.9+
* Docker Desktop (optional but recommended)
* PostgreSQL (if running without Docker)
* Google Chrome (latest)

---

##  Run with Docker (Recommended)

### 1. Clone repo

```bash
git clone https://github.com/Yaroslav-Kirsenko/Scraper
cd Scraper
```

### 2. Run

```bash
docker-compose up --build
```

### 3. Open Swagger

http://localhost:8080/swagger-ui.html

---

## Run Locally (Manual)

### 1. Create DB

```sql
CREATE DATABASE scraper;
```

---

### 2. Configure application.properties

```
spring.datasource.url=jdbc:postgresql://localhost:5432/scraper
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```

---

### 3. Build & run

```bash
mvn clean package -DskipTests
java -jar target/*.jar
```

---

## Google Sheets Setup

### 1. Create Google Cloud Project

Go to:
https://console.cloud.google.com/

---

### 2. Enable API

Enable:

* Google Sheets API

---

### 3. Create Service Account

* Go to **IAM & Admin → Service Accounts**
* Create new account
* Generate JSON key

---

### 4. Add credentials to project

Place file:

```
src/main/resources/google-credentials.json
```

---

### 5. Share your Google Sheet

Open your sheet and click **Share**

Add service account email like:

```
your-service-account@project-id.iam.gserviceaccount.com
```

Give role:

```
Editor
```

---

### 6. Configure application

```
google.sheets.enabled=true
google.sheets.spreadsheet-id=YOUR_SPREADSHEET_ID
google.sheets.sheet-name=Scraper Logs
```

---

## Common Issues

### 400 Unable to parse range

Fix:

```
'Sheet Name'
```

Example:

```
'Scraper Logs'
```

---

### 403 Permission denied

* Sheet not shared with service account

---

### Credentials not found

* File missing in resources folder

---

## Stop Docker

```bash
docker-compose down
```

---

## Remove DB volume

```bash
docker-compose down -v
```

---
