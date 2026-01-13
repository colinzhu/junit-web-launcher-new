# Implementation Plan: JUnit Web Launcher

## Overview

This implementation plan breaks down the JUnit Web Launcher into incremental coding tasks. The approach follows a bottom-up strategy: building core services first, then adding web endpoints, and finally implementing optional features. Each task builds on previous work to ensure continuous integration and validation.

## Tasks

- [x] 1. Set up project structure and dependencies
  - Create Spring Boot Maven/Gradle project with Java 17+
  - Add dependencies: Spring Boot Web, JUnit Platform Launcher API, Allure Java, jqwik for property testing
  - Configure application.properties for server port, file storage paths
  - Create package structure: controller, service, model, config
  - _Requirements: All_

- [x] 2. Implement core data models
  - [x] 2.1 Create data model classes
    - Implement TestTree, TestClass, TestMethod POJOs
    - Implement ExecutionRequest, ExecutionResponse, ExecutionStatus
    - Implement ReportMetadata, LogFileMetadata
    - Add proper equals/hashCode/toString methods
    - _Requirements: 1.1, 1.3, 1.4, 1.5_
  
  - [ ]* 2.2 Write property test for data model completeness
    - **Property 3: Test Class Data Completeness**
    - **Validates: Requirements 1.4, 1.5**

- [x] 3. Implement Test Discovery Service
  - [x] 3.1 Create TestDiscoveryService interface and implementation
    - Use JUnit Platform LauncherFactory to create Launcher
    - Build LauncherDiscoveryRequest with package selectors
    - Implement discoverTests() method to scan classpath
    - Parse TestPlan into TestTree structure
    - Handle empty classpath gracefully
    - _Requirements: 1.1, 1.2, 1.3_
  
  - [ ]* 3.2 Write property test for package filtering
    - **Property 1: Package Filter Restricts Discovery**
    - **Validates: Requirements 1.2**
  
  - [ ]* 3.3 Write property test for test organization
    - **Property 2: Discovery Organizes Tests by Class**
    - **Validates: Requirements 1.3**
  
  - [ ]* 3.4 Write unit tests for discovery edge cases
    - Test with empty classpath
    - Test with invalid package filter
    - Test with nested packages
    - _Requirements: 1.1, 1.2_

- [x] 4. Checkpoint - Verify discovery works
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Implement Test Execution Service
  - [x] 5.1 Create TestExecutionService interface and implementation
    - Implement executeTests() to run selected tests in background thread
    - Use LauncherDiscoveryRequest with selectUniqueId() for each test
    - Generate unique execution IDs (UUID with timestamp)
    - Maintain map of active executions
    - Implement custom TestExecutionListener to capture events
    - _Requirements: 3.1, 3.4_
  
  - [x] 5.2 Implement cancellation support
    - Implement cancelExecution() method
    - Use Thread.interrupt() for cancellation
    - Update execution status to CANCELLED
    - Ensure cleanup of resources
    - _Requirements: 4.2, 4.4_
  
  - [ ]* 5.3 Write property test for executor runs selected tests
    - **Property 6: Executor Runs Selected Tests**
    - **Validates: Requirements 3.1**
  
  - [ ]* 5.4 Write property test for cancellation
    - **Property 10: Cancellation Terminates Execution**
    - **Validates: Requirements 4.2**
  
  - [ ]* 5.5 Write property test for test result status validity
    - **Property 8: Test Result Status Validity**
    - **Validates: Requirements 3.4**

- [x] 6. Implement Log Streaming Service
  - [x] 6.1 Create LogStreamingService with SSE support
    - Implement streamLogs() returning SseEmitter
    - Maintain map of execution IDs to emitters
    - Implement publishLog() to send messages to all emitters
    - Capture System.out and System.err using custom PrintStream
    - Handle emitter timeout and completion events
    - _Requirements: 3.2, 3.5_
  
  - [x] 6.2 Integrate log capture with test execution
    - Redirect stdout/stderr during test execution
    - Forward captured logs to LogStreamingService
    - Restore original streams after execution
    - _Requirements: 3.2_
  
  - [ ]* 6.3 Write property test for log streaming
    - **Property 7: Log Streaming During Execution**
    - **Validates: Requirements 3.2**
  
  - [ ]* 6.4 Write property test for log chronological order
    - **Property 9: Log Message Chronological Order**
    - **Validates: Requirements 3.5**
  
  - [ ]* 6.5 Write property test for cancellation preserves logs
    - **Property 11: Cancellation Preserves Logs**
    - **Validates: Requirements 4.4**

- [x] 7. Checkpoint - Verify execution and streaming work
  - Ensure all tests pass, ask the user if questions arise.

- [-] 8. Implement Allure integration
  - [x] 8.1 Configure Allure for test execution
    - Add Allure lifecycle configuration
    - Set up results directory per execution
    - Configure Allure annotations/listeners
    - _Requirements: 5.1_
  
  - [x] 8.2 Implement Report Service for generation
    - Create ReportService interface and implementation
    - Implement generateReport() using ProcessBuilder to call Allure CLI
    - Generate report with timestamp-based naming (YYYY-MM-DD_HH-MM-SS)
    - Store report metadata as JSON
    - Handle Allure CLI errors gracefully
    - _Requirements: 5.1, 5.2, 5.3_
  
  - [ ]* 8.3 Write property test for report generation
    - **Property 12: Report Generation After Completion**
    - **Validates: Requirements 5.1**
  
  - [ ]* 8.4 Write property test for report timestamp naming
    - **Property 13: Report Timestamp Naming**
    - **Validates: Requirements 5.2**
  
  - [ ]* 8.5 Write property test for report accessibility
    - **Property 14: Report Accessibility After Generation**
    - **Validates: Requirements 5.3**

- [x] 9. Implement Archive Service
  - [x] 9.1 Create ArchiveService for logs and reports
    - Implement archiveLogs() to save log files with timestamps
    - Implement listLogFiles() and getLogFile()
    - Implement listReports() with metadata
    - Implement createReportArchive() to ZIP reports
    - Sort lists by timestamp descending
    - _Requirements: 5.5, 6.1, 6.2, 7.1, 7.2_
  
  - [ ]* 9.2 Write property test for report persistence
    - **Property 15: Report Persistence**
    - **Validates: Requirements 5.5**
  
  - [ ]* 9.3 Write property test for historical reports list
    - **Property 16: Historical Reports List Completeness**
    - **Validates: Requirements 6.1**
  
  - [ ]* 9.4 Write property test for report metadata completeness
    - **Property 17: Report Metadata Completeness**
    - **Validates: Requirements 6.2**
  
  - [ ]* 9.5 Write property test for reports sorted descending
    - **Property 20: Reports Sorted by Timestamp Descending**
    - **Validates: Requirements 6.5**
  
  - [ ]* 9.6 Write property test for log file timestamp naming
    - **Property 21: Log File Timestamp Naming**
    - **Validates: Requirements 7.1**
  
  - [ ]* 9.7 Write property test for historical logs list
    - **Property 22: Historical Logs List Completeness**
    - **Validates: Requirements 7.2**
  
  - [ ]* 9.8 Write property test for logs sorted descending
    - **Property 25: Logs Sorted by Timestamp Descending**
    - **Validates: Requirements 7.5**

- [-] 10. Implement Web Controller endpoints
  - [x] 10.1 Create REST controller for test discovery
    - Implement GET /api/discover endpoint
    - Accept optional packageFilter query parameter
    - Return TestTree JSON response
    - Handle errors with appropriate HTTP status codes
    - _Requirements: 1.1, 1.2_
  
  - [x] 10.2 Create REST controller for test execution
    - Implement POST /api/execute endpoint
    - Accept ExecutionRequest with selected test IDs
    - Return ExecutionResponse with execution ID
    - Implement GET /api/stream/{executionId} for SSE
    - Implement POST /api/cancel/{executionId}
    - _Requirements: 3.1, 3.2, 4.2_
  
  - [x] 10.3 Create REST controller for reports
    - Implement GET /api/reports to list all reports
    - Implement GET /api/reports/{reportId} to view report
    - Implement GET /api/reports/{reportId}/download for ZIP download
    - Serve static Allure HTML files
    - _Requirements: 6.1, 6.2, 6.3, 6.4_
  
  - [x] 10.4 Create REST controller for logs
    - Implement GET /api/logs to list all log files
    - Implement GET /api/logs/{logId} to view log content
    - Implement GET /api/logs/{logId}/download
    - _Requirements: 7.2, 7.3, 7.4_
  
  - [ ]* 10.5 Write property test for historical report retrieval
    - **Property 18: Historical Report Retrieval**
    - **Validates: Requirements 6.3**
  
  - [ ]* 10.6 Write property test for historical report download
    - **Property 19: Historical Report Download**
    - **Validates: Requirements 6.4**
  
  - [ ]* 10.7 Write property test for historical log retrieval
    - **Property 23: Historical Log Retrieval**
    - **Validates: Requirements 7.3**
  
  - [ ]* 10.8 Write property test for historical log download
    - **Property 24: Historical Log Download**
    - **Validates: Requirements 7.4**

- [x] 11. Checkpoint - Verify all core endpoints work
  - Ensure all tests pass, ask the user if questions arise.

- [x] 12. Implement frontend web interface
  - [x] 12.1 Create HTML page for test discovery and selection
    - Display test tree with expandable classes
    - Add checkboxes for test classes and methods
    - Implement class-level selection synchronization
    - Add package filter input field
    - Add Run button (enabled when tests selected)
    - _Requirements: 1.1, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_
  
  - [x] 12.2 Create JavaScript for test execution and streaming
    - Implement EventSource for SSE log streaming
    - Display real-time logs in scrollable div
    - Show execution status (RUNNING, COMPLETED, CANCELLED)
    - Add Cancel button during execution
    - Display test results with pass/fail/skip indicators
    - _Requirements: 3.2, 3.3, 3.4, 4.1_
  
  - [x] 12.3 Create HTML page for reports and logs
    - Display list of historical reports with timestamps
    - Add view and download buttons for each report
    - Display list of historical log files
    - Add view and download buttons for each log
    - Embed Allure report viewer in iframe
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 7.2, 7.3, 7.4_
  
  - [ ]* 12.4 Write property test for class selection synchronization
    - **Property 4: Class Selection Synchronization**
    - **Validates: Requirements 2.4, 2.5**
  
  - [ ]* 12.5 Write property test for run button enablement
    - **Property 5: Run Button Enablement**
    - **Validates: Requirements 2.6**

- [ ] 13. Implement report combination feature (Optional)
  - [ ] 13.1 Add report combination to ReportService
    - Implement combineReports() method
    - Copy all allure-results from selected reports to temp directory
    - Generate combined report with "combined" in name
    - Store combined report metadata with source report IDs
    - _Requirements: 8.3, 8.4, 8.5_
  
  - [ ] 13.2 Add report combination endpoint
    - Implement POST /api/reports/combine
    - Accept list of report IDs
    - Return combined report metadata
    - _Requirements: 8.3, 8.4_
  
  - [ ] 13.3 Add UI for report combination
    - Add checkboxes to report list for selection
    - Add "Combine Selected" button
    - Display combined report after generation
    - _Requirements: 8.2, 8.4_
  
  - [ ]* 13.4 Write property test for report combination
    - **Property 26: Report Combination Merges All Tests**
    - **Validates: Requirements 8.3**
  
  - [ ]* 13.5 Write property test for combined report accessibility
    - **Property 27: Combined Report Accessibility**
    - **Validates: Requirements 8.4**
  
  - [ ]* 13.6 Write property test for combined report naming
    - **Property 28: Combined Report Naming Convention**
    - **Validates: Requirements 8.5**

- [ ] 14. Implement failed test re-run feature (Optional)
  - [ ] 14.1 Add failed test extraction to ReportService
    - Parse Allure results to identify failed tests
    - Extract unique IDs of failed tests
    - Implement getFailedTests() method
    - _Requirements: 9.2_
  
  - [ ] 14.2 Add re-run endpoint
    - Implement POST /api/rerun/{reportId}
    - Extract failed tests from original report
    - Execute only those tests
    - Generate new report for re-run
    - _Requirements: 9.2, 9.3_
  
  - [ ] 14.3 Add UI for re-run with combination
    - Add "Re-run Failed Tests" button on report view
    - Show re-run progress
    - Offer to combine re-run with original report
    - Display combined report showing updated statuses
    - _Requirements: 9.1, 9.4_
  
  - [ ]* 14.4 Write property test for re-run executes only failed tests
    - **Property 29: Re-run Executes Only Failed Tests**
    - **Validates: Requirements 9.2**
  
  - [ ]* 14.5 Write property test for re-run generates new report
    - **Property 30: Re-run Generates New Report**
    - **Validates: Requirements 9.3**
  
  - [ ]* 14.6 Write property test for combined re-run shows updated status
    - **Property 31: Combined Re-run Shows Updated Status**
    - **Validates: Requirements 9.5**

- [ ] 15. Implement Allure history tracking
  - [ ] 15.1 Add history folder management
    - Before generating report, copy history folder from previous report to current allure-results
    - Preserve history folder from generated report for next run
    - Enable Allure trend charts and historical comparison
    - _Requirements: 6.2_

- [ ] 16. Add error handling and validation
  - [ ] 16.1 Add comprehensive error handling
    - Handle missing Allure CLI with clear error message
    - Handle invalid test IDs with 400 Bad Request
    - Handle concurrent execution attempts with 409 Conflict
    - Handle missing reports/logs with 404 Not Found
    - Add global exception handler for unexpected errors
    - _Requirements: All_
  
  - [ ]* 16.2 Write unit tests for error scenarios
    - Test empty classpath handling
    - Test invalid package filter
    - Test missing Allure CLI
    - Test concurrent execution prevention
    - Test SSE client disconnect handling
    - _Requirements: All_

- [ ] 17. Final checkpoint and integration testing
  - [ ] 17.1 Run full integration test suite
    - Test complete workflow: discover → select → execute → view report
    - Test cancellation during execution
    - Test log streaming with multiple clients
    - Test report combination with multiple reports
    - Test failed test re-run workflow
    - _Requirements: All_
  
  - [ ] 17.2 Verify all property tests pass with 100+ iterations
    - Run all property-based tests with full iteration count
    - Ensure all tests pass consistently
    - _Requirements: All_

- [ ] 18. Documentation and deployment preparation
  - [ ] 18.1 Create README with setup instructions
    - Document prerequisites (Java, Maven/Gradle, Allure CLI)
    - Provide installation and running instructions
    - Document API endpoints
    - Include screenshots of web interface
  
  - [ ] 18.2 Add configuration documentation
    - Document application.properties settings
    - Explain file storage configuration
    - Document optional features (combination, re-run)

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation throughout development
- Property tests validate universal correctness properties with 100+ iterations
- Unit tests validate specific examples and edge cases
- Optional features (tasks 13-14) can be implemented after core functionality is complete
- The implementation follows a bottom-up approach: services → controllers → frontend
