# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This is a Spring Boot web application that provides a web-based interface for discovering, selecting, and executing JUnit 5 test cases with automatic Allure report generation. The application uses the JUnit Platform Launcher API to programmatically discover and run tests without requiring the Allure CLI.

## Common Commands

**Build the project:**
```bash
mvn clean install
```

**Run the application:**
```bash
mvn spring-boot:run
```

The application starts on `http://localhost:8080`.

**Run tests:**
```bash
mvn test
```

**Run a single test class:**
```bash
mvn test -Dtest=SampleTest
```

## Architecture

### High-Level Flow

1. **Test Discovery** → `TestDiscoveryServiceImpl` uses JUnit Platform Launcher API to scan classpath and build a test tree
2. **Test Execution** → `TestExecutionServiceImpl` executes selected tests in background threads with real-time log streaming via SSE
3. **Allure Integration** → `AllureConfigurationService` configures Allure lifecycle per execution; `ReportServiceImpl` generates reports programmatically using `allure-generator`
4. **Report Management** → Reports are stored with metadata and support historical trend tracking via Allure's history feature

### Key Components

**Controllers** (`src/main/java/com/junit/launcher/controller/`):
- `TestDiscoveryController` - `/api/discover` - Discovers tests with optional package filtering
- `TestExecutionController` - `/api/execute`, `/api/stream/{id}`, `/api/status/{id}`, `/api/cancel/{id}` - Execute tests and stream logs via SSE
- `ReportController` - `/api/reports/*` - List, view, download reports, and re-run failed tests
- `LogController` - Log streaming endpoints

**Services** (`src/main/java/com/junit/launcher/service/`):
- `TestDiscoveryServiceImpl` - Uses `LauncherFactory.create()` and `LauncherDiscoveryRequest` to discover tests via JUnit Platform API
- `TestExecutionServiceImpl` - Executes tests using `Launcher.execute()` with custom `TestExecutionListener` that records results to Allure
- `ReportServiceImpl` - Generates Allure reports using `ReportGenerator` (programmatic, no CLI required)
- `AllureConfigurationService` - Sets `allure.results.directory` system property per execution
- `LogStreamingService` - SSE-based log streaming for real-time execution monitoring

**Models** (`src/main/java/com/junit/launcher/model/`):
- `TestTree`, `TestClass`, `TestMethod` - Hierarchical test discovery structure
- `ExecutionRequest`, `ExecutionResponse`, `ExecutionStatus` - Execution state management
- `ReportMetadata` - Report metadata with test counts (passed/failed/skipped)

**Configuration** (`src/main/java/com/junit/launcher/config/`):
- `StorageProperties` - Configurable paths for reports, logs, and Allure results
- `AllureProperties` - Allure configuration (uses Java API, not CLI)
- `AsyncConfig` - Async request timeout configuration

### Storage Structure

```
storage/
├── reports/           # Generated Allure reports (HTML)
├── logs/              # Execution logs
└── allure-results/    # Raw Allure result JSON files per execution ID
```

### Allure Report Generation Flow

1. Test execution writes result files to `storage/allure-results/{executionId}/`
2. `ReportServiceImpl.generateReport()` copies history from previous report
3. `ReportGenerator` (from `allure-generator` dependency) generates HTML report
4. Report stored in `storage/reports/{reportId}/` with `metadata.json`

### Frontend

- Static files in `src/main/resources/static/`
- `index.html` - Test discovery and execution UI (Alpine.js)
- `reports.html` - Report browsing and management
- JavaScript in `src/main/resources/static/js/` (referenced in HTML)

### Sample Test

`SampleTest.java` is included for testing discovery functionality. It contains two simple `@Test` methods.

## Configuration

Edit `src/main/resources/application.properties`:
- `server.port` - Server port (default: 8080)
- `storage.reports.path` - Report storage path
- `storage.logs.path` - Log storage path
- `storage.allure-results.path` - Allure results storage path
- `spring.mvc.async.request-timeout` - SSE timeout (default: 1800000ms)

## Key Dependencies

- `junit-platform-launcher` - JUnit 5 Platform API for test discovery/execution
- `allure-java-commons`, `allure-junit5` - Allure annotations and integration
- `allure-generator` - Programmatic report generation (no CLI required)
- `spring-boot-starter-web` - REST API and static file serving

## Important Implementation Details

### Test Discovery
- Uses `LauncherFactory.create()` to create JUnit launcher
- `DiscoverySelectors.selectClasspathRoots()` for full classpath scanning
- `DiscoverySelectors.selectPackage()` for package filtering
- Parses `TestPlan` into hierarchical `TestTree` structure

### Test Execution
- Runs in background thread per execution ID
- `CustomTestExecutionListener` implements `TestExecutionListener` interface
- Records test start/finish/skip events to Allure lifecycle
- Supports cancellation via thread interruption
- Real-time logs via `LogStreamingService` using `SseEmitter`

### Report Generation
- Programmatic generation using `io.qameta.allure.ReportGenerator`
- No Allure CLI dependency required
- History tracking via `history` folder copied between reports
- Failed test extraction parses `-result.json` files for `status: "failed"`

### Security
- Path traversal protection in `ReportController.getReportFile()` using `Path.startsWith()` check
- Validates report IDs before serving files

## Testing Notes

The `SampleTest.java` class can be used to verify discovery and execution functionality. After running tests, check:
- `/api/discover` - Should return test tree with `SampleTest`
- `/api/execute` - Can execute `SampleTest.testOne` and `SampleTest.testTwo`
- `/api/reports` - Should show generated reports with test results
