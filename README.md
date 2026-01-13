# JUnit Web Launcher

A web-based application for discovering, selecting, and executing JUnit test cases with Allure reporting.

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Allure CLI (for report generation)

## Installation

### Install Allure CLI

**macOS:**
```bash
brew install allure
```

**Windows:**
```bash
scoop install allure
```

**Linux:**
Download from [Allure releases](https://github.com/allure-framework/allure2/releases)

## Building

```bash
mvn clean install
```

## Running

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## Configuration

Edit `src/main/resources/application.properties` to customize:
- Server port
- File storage paths
- Allure CLI path

## Project Structure

```
src/main/java/com/junit/launcher/
├── controller/     # REST endpoints
├── service/        # Business logic
├── model/          # Data models
└── config/         # Configuration classes
```

## Features

- Test discovery with package filtering
- Real-time test execution with log streaming
- Allure report generation
- Historical report and log management
- Test execution cancellation
- Report combination (optional)
- Failed test re-run (optional)
