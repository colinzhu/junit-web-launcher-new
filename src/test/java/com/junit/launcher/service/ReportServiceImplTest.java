package com.junit.launcher.service;

import com.junit.launcher.config.AllureProperties;
import com.junit.launcher.config.StorageProperties;
import com.junit.launcher.model.ReportMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReportServiceImpl.
 */
class ReportServiceImplTest {
    
    private ReportService reportService;
    private AllureConfigurationService allureConfigurationService;
    private StorageProperties storageProperties;
    private AllureProperties allureProperties;
    
    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        storageProperties.setReportsPath("./test-storage/reports");
        storageProperties.setAllureResultsPath("./test-storage/allure-results");
        
        allureProperties = new AllureProperties();
        allureProperties.setPath("allure");
        
        allureConfigurationService = new AllureConfigurationService(storageProperties);
        reportService = new ReportServiceImpl(allureProperties, storageProperties, allureConfigurationService);
    }
    
    @AfterEach
    void tearDown() throws IOException {
        // Clean up test directories
        Path testStorage = Paths.get("./test-storage");
        if (Files.exists(testStorage)) {
            deleteDirectory(testStorage);
        }
    }
    
    @Test
    void testGenerateReport_throwsExceptionForNonExistentExecution() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            reportService.generateReport("non-existent-execution");
        });
        
        assertTrue(exception.getMessage().contains("Results directory not found"));
    }
    
    @Test
    void testListReports_returnsEmptyListWhenNoReports() {
        List<ReportMetadata> reports = reportService.listReports();
        
        assertNotNull(reports);
        assertEquals(0, reports.size());
    }
    
    @Test
    void testGetReportMetadata_returnsNullForNonExistentReport() {
        ReportMetadata metadata = reportService.getReportMetadata("non-existent-report");
        
        assertEquals(null, metadata);
    }
    
    @Test
    void testCombineReports_throwsExceptionForLessThanTwoReports() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            reportService.combineReports(List.of("report1"));
        });
        
        assertTrue(exception.getMessage().contains("At least 2 reports are required"));
    }
    
    @Test
    void testCombineReports_throwsExceptionForNonExistentReport() {
        Exception exception = assertThrows(Exception.class, () -> {
            reportService.combineReports(List.of("report1", "report2"));
        });
        
        assertTrue(exception.getMessage().contains("Report not found"));
    }
    
    @Test
    void testGenerateReport_createsReportWithTimestampNaming() throws Exception {
        // Create a mock execution with results directory
        String executionId = "test-execution-123";
        Path resultsDir = allureConfigurationService.getResultsDirectory(executionId);
        Files.createDirectories(resultsDir);
        
        // Create a dummy result file to simulate test execution
        createDummyAllureResult(resultsDir, "passed");
        
        // Note: This test will fail if Allure CLI is not installed
        // We're testing the error handling in that case
        try {
            ReportMetadata metadata = reportService.generateReport(executionId);
            
            // If Allure is installed, verify the report metadata
            assertNotNull(metadata);
            assertNotNull(metadata.getReportId());
            assertTrue(metadata.getReportId().startsWith("allure-report-"));
            assertTrue(metadata.getReportId().matches("allure-report-\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}"));
            assertEquals(executionId, metadata.getExecutionId());
            assertNotNull(metadata.getTimestamp());
            assertFalse(metadata.isCombined());
            
        } catch (RuntimeException e) {
            // Expected if Allure CLI is not installed
            assertTrue(e.getMessage().contains("Failed to execute Allure CLI") || 
                      e.getMessage().contains("Allure report generation failed"));
        }
    }
    
    @Test
    void testGetFailedTests_returnsFailedTestIds() throws Exception {
        // Create a mock execution with results directory
        String executionId = "test-execution-failed";
        Path resultsDir = allureConfigurationService.getResultsDirectory(executionId);
        Files.createDirectories(resultsDir);
        
        // Create dummy result files with different statuses
        createDummyAllureResultWithTestCaseId(resultsDir, "passed", "test1", "TestClass.testMethod1");
        createDummyAllureResultWithTestCaseId(resultsDir, "failed", "test2", "TestClass.testMethod2");
        createDummyAllureResultWithTestCaseId(resultsDir, "skipped", "test3", "TestClass.testMethod3");
        createDummyAllureResultWithTestCaseId(resultsDir, "failed", "test4", "TestClass.testMethod4");
        
        // Create a mock report metadata
        ReportMetadata metadata = new ReportMetadata();
        metadata.setReportId("test-report");
        metadata.setExecutionId(executionId);
        metadata.setTimestamp("2024-01-01_12-00-00");
        metadata.setReportPath("./test-storage/reports/test-report");
        
        // Create report directory and save metadata
        Path reportDir = Paths.get(metadata.getReportPath());
        Files.createDirectories(reportDir);
        Path metadataFile = reportDir.resolve("metadata.json");
        Files.writeString(metadataFile, 
            "{\"reportId\":\"test-report\",\"executionId\":\"" + executionId + "\",\"timestamp\":\"2024-01-01_12-00-00\"}");
        
        // Test getFailedTests
        List<String> failedTests = reportService.getFailedTests("test-report");
        
        assertNotNull(failedTests);
        assertEquals(2, failedTests.size());
        assertTrue(failedTests.contains("TestClass.testMethod2"));
        assertTrue(failedTests.contains("TestClass.testMethod4"));
    }
    
    @Test
    void testGetFailedTests_throwsExceptionForNonExistentReport() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            reportService.getFailedTests("non-existent-report");
        });
        
        assertTrue(exception.getMessage().contains("Report not found"));
    }
    
    @Test
    void testHistoryFolderManagement_copiesHistoryFromPreviousReport() throws Exception {
        // Create first execution with results
        String firstExecutionId = "test-execution-1";
        Path firstResultsDir = allureConfigurationService.getResultsDirectory(firstExecutionId);
        Files.createDirectories(firstResultsDir);
        createDummyAllureResult(firstResultsDir, "passed");
        
        // Create first report directory with history folder
        String firstReportId = "allure-report-2024-01-01_10-00-00";
        Path firstReportDir = Paths.get(storageProperties.getReportsPath(), firstReportId);
        Files.createDirectories(firstReportDir);
        
        // Create history folder in first report
        Path firstHistoryDir = firstReportDir.resolve("history");
        Files.createDirectories(firstHistoryDir);
        Files.writeString(firstHistoryDir.resolve("history.json"), "{\"history\":\"data1\"}");
        Files.writeString(firstHistoryDir.resolve("trend.json"), "{\"trend\":\"data1\"}");
        
        // Create metadata for first report
        ReportMetadata firstMetadata = new ReportMetadata();
        firstMetadata.setReportId(firstReportId);
        firstMetadata.setExecutionId(firstExecutionId);
        firstMetadata.setTimestamp("2024-01-01_10-00-00");
        firstMetadata.setReportPath(firstReportDir.toString());
        firstMetadata.setTotalTests(1);
        firstMetadata.setPassedTests(1);
        
        Path firstMetadataFile = firstReportDir.resolve("metadata.json");
        String escapedPath = firstReportDir.toString().replace("\\", "\\\\");
        Files.writeString(firstMetadataFile, 
            "{\"reportId\":\"" + firstReportId + "\",\"executionId\":\"" + firstExecutionId + 
            "\",\"timestamp\":\"2024-01-01_10-00-00\",\"reportPath\":\"" + escapedPath + 
            "\",\"totalTests\":1,\"passedTests\":1,\"failedTests\":0,\"skippedTests\":0,\"combined\":false}");
        
        // Create second execution with results
        String secondExecutionId = "test-execution-2";
        Path secondResultsDir = allureConfigurationService.getResultsDirectory(secondExecutionId);
        Files.createDirectories(secondResultsDir);
        createDummyAllureResult(secondResultsDir, "failed");
        
        // Verify that history folder doesn't exist in second results directory initially
        Path secondHistoryDir = secondResultsDir.resolve("history");
        assertFalse(Files.exists(secondHistoryDir));
        
        // Generate second report (this should copy history from first report)
        try {
            ReportMetadata secondMetadata = reportService.generateReport(secondExecutionId);
            
            // If Allure is installed, verify history was copied
            assertTrue(Files.exists(secondHistoryDir));
            assertTrue(Files.exists(secondHistoryDir.resolve("history.json")));
            assertTrue(Files.exists(secondHistoryDir.resolve("trend.json")));
            
            // Verify content was copied correctly
            String historyContent = Files.readString(secondHistoryDir.resolve("history.json"));
            assertEquals("{\"history\":\"data1\"}", historyContent);
            
            String trendContent = Files.readString(secondHistoryDir.resolve("trend.json"));
            assertEquals("{\"trend\":\"data1\"}", trendContent);
            
        } catch (RuntimeException e) {
            // Expected if Allure CLI is not installed
            // But history should still be copied to results directory
            assertTrue(Files.exists(secondHistoryDir));
            assertTrue(Files.exists(secondHistoryDir.resolve("history.json")));
            assertTrue(Files.exists(secondHistoryDir.resolve("trend.json")));
        }
    }
    
    @Test
    void testHistoryFolderManagement_handlesNoHistoryGracefully() throws Exception {
        // Create execution with results but no previous reports
        String executionId = "test-execution-no-history";
        Path resultsDir = allureConfigurationService.getResultsDirectory(executionId);
        Files.createDirectories(resultsDir);
        createDummyAllureResult(resultsDir, "passed");
        
        // Verify that history folder doesn't exist initially
        Path historyDir = resultsDir.resolve("history");
        assertFalse(Files.exists(historyDir));
        
        // Generate report (should handle no previous reports gracefully)
        try {
            ReportMetadata metadata = reportService.generateReport(executionId);
            
            // History folder should still not exist since there were no previous reports
            assertFalse(Files.exists(historyDir));
            
        } catch (RuntimeException e) {
            // Expected if Allure CLI is not installed
            // History folder should still not exist
            assertFalse(Files.exists(historyDir));
        }
    }
    
    private void createDummyAllureResult(Path resultsDir, String status) throws IOException {
        String resultJson = String.format(
            "{\"uuid\":\"test-uuid\",\"name\":\"Test\",\"status\":\"%s\"}", status
        );
        Path resultFile = resultsDir.resolve("test-result.json");
        Files.writeString(resultFile, resultJson);
    }
    
    private void createDummyAllureResultWithTestCaseId(Path resultsDir, String status, String uuid, String testCaseId) throws IOException {
        String resultJson = String.format(
            "{\"uuid\":\"%s\",\"name\":\"Test\",\"status\":\"%s\",\"testCaseId\":\"%s\"}", 
            uuid, status, testCaseId
        );
        Path resultFile = resultsDir.resolve(uuid + "-result.json");
        Files.writeString(resultFile, resultJson);
    }
    
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // Ignore
                    }
                });
        }
    }
}
