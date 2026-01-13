package com.junit.launcher.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.junit.launcher.config.AllureProperties;
import com.junit.launcher.config.StorageProperties;
import com.junit.launcher.model.ReportMetadata;

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
        Path resultsDir = allureConfigurationService.configureAllureForExecution(executionId);
        
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
    
    private void createDummyAllureResult(Path resultsDir, String status) throws IOException {
        String resultJson = String.format(
            "{\"uuid\":\"test-uuid\",\"name\":\"Test\",\"status\":\"%s\"}", status
        );
        Path resultFile = resultsDir.resolve("test-result.json");
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
