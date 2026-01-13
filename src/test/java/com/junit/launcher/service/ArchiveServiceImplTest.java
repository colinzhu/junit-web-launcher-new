package com.junit.launcher.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.junit.launcher.config.StorageProperties;
import com.junit.launcher.model.LogFileMetadata;
import com.junit.launcher.model.ReportMetadata;

/**
 * Unit tests for ArchiveServiceImpl.
 */
class ArchiveServiceImplTest {
    
    @TempDir
    Path tempDir;
    
    private ArchiveService archiveService;
    private StorageProperties storageProperties;
    private ReportService mockReportService;
    
    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        storageProperties.setLogsPath(tempDir.resolve("logs").toString());
        storageProperties.setReportsPath(tempDir.resolve("reports").toString());
        storageProperties.setAllureResultsPath(tempDir.resolve("allure-results").toString());
        
        // Create a simple mock ReportService
        mockReportService = new ReportService() {
            @Override
            public ReportMetadata generateReport(String executionId) throws Exception {
                return null;
            }
            
            @Override
            public List<ReportMetadata> listReports() {
                return List.of();
            }
            
            @Override
            public ReportMetadata combineReports(List<String> reportIds) throws Exception {
                return null;
            }
            
            @Override
            public ReportMetadata getReportMetadata(String reportId) {
                return null;
            }
            
            @Override
            public List<String> getFailedTests(String reportId) throws Exception {
                return List.of();
            }
        };
        
        archiveService = new ArchiveServiceImpl(storageProperties, mockReportService);
    }
    
    @AfterEach
    void tearDown() throws Exception {
        // Cleanup is handled by @TempDir
    }
    
    @Test
    void testArchiveLogs_CreatesLogFile() throws Exception {
        // Given
        String executionId = "test-execution-123";
        String logs = "Test log line 1\nTest log line 2\nTest log line 3";
        
        // When
        archiveService.archiveLogs(executionId, logs);
        
        // Then
        Path logsDir = Paths.get(storageProperties.getLogsPath());
        assertTrue(Files.exists(logsDir), "Logs directory should be created");
        
        List<Path> logFiles = Files.list(logsDir)
            .filter(p -> p.getFileName().toString().endsWith(".log"))
            .toList();
        
        assertEquals(1, logFiles.size(), "Should create one log file");
        
        String savedContent = Files.readString(logFiles.get(0));
        assertEquals(logs, savedContent, "Log content should match");
    }
    
    @Test
    void testListLogFiles_EmptyDirectory() {
        // When
        List<LogFileMetadata> logFiles = archiveService.listLogFiles();
        
        // Then
        assertNotNull(logFiles);
        assertTrue(logFiles.isEmpty(), "Should return empty list for non-existent directory");
    }
    
    @Test
    void testListLogFiles_ReturnsAllLogs() throws Exception {
        // Given
        archiveService.archiveLogs("exec-1", "Log 1");
        archiveService.archiveLogs("exec-2", "Log 2");
        archiveService.archiveLogs("exec-3", "Log 3");
        
        // When
        List<LogFileMetadata> logFiles = archiveService.listLogFiles();
        
        // Then
        assertEquals(3, logFiles.size(), "Should return all log files");
        
        // Verify sorted by timestamp descending (newest first)
        for (int i = 0; i < logFiles.size() - 1; i++) {
            String current = logFiles.get(i).getTimestamp();
            String next = logFiles.get(i + 1).getTimestamp();
            assertTrue(current.compareTo(next) >= 0, 
                "Log files should be sorted by timestamp descending");
        }
    }
    
    @Test
    void testGetLogFile_ExistingFile() throws Exception {
        // Given
        String executionId = "test-execution";
        String expectedLogs = "Test log content";
        archiveService.archiveLogs(executionId, expectedLogs);
        
        // Get the created log file name
        List<LogFileMetadata> logFiles = archiveService.listLogFiles();
        String logFileId = logFiles.get(0).getLogId();
        
        // When
        String actualLogs = archiveService.getLogFile(logFileId);
        
        // Then
        assertNotNull(actualLogs);
        assertEquals(expectedLogs, actualLogs);
    }
    
    @Test
    void testGetLogFile_NonExistentFile() throws Exception {
        // When
        String logs = archiveService.getLogFile("non-existent-file.log");
        
        // Then
        assertNull(logs, "Should return null for non-existent file");
    }
    
    @Test
    void testCreateReportArchive_NonExistentReport() {
        // When/Then
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            archiveService.createReportArchive("non-existent-report");
        });
        
        assertTrue(exception.getMessage().contains("Report not found"));
    }
    
    @Test
    void testCreateReportArchive_CreatesZipFile() throws Exception {
        // Given
        String reportId = "test-report";
        Path reportDir = Paths.get(storageProperties.getReportsPath(), reportId);
        Files.createDirectories(reportDir);
        
        // Create some test files in the report directory
        Files.writeString(reportDir.resolve("index.html"), "<html>Test Report</html>");
        Files.writeString(reportDir.resolve("data.json"), "{\"test\": \"data\"}");
        
        Path subDir = reportDir.resolve("assets");
        Files.createDirectories(subDir);
        Files.writeString(subDir.resolve("style.css"), "body { color: black; }");
        
        // When
        Path zipFile = archiveService.createReportArchive(reportId);
        
        // Then
        assertNotNull(zipFile);
        assertTrue(Files.exists(zipFile), "ZIP file should be created");
        assertTrue(zipFile.getFileName().toString().endsWith(".zip"), "Should be a ZIP file");
        assertTrue(Files.size(zipFile) > 0, "ZIP file should not be empty");
    }
    
    @Test
    void testListReports_DelegatesToReportService() {
        // When
        List<ReportMetadata> reports = archiveService.listReports();
        
        // Then
        assertNotNull(reports);
        assertEquals(0, reports.size(), "Should delegate to ReportService");
    }
}
