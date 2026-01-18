package com.junit.launcher.controller;

import com.junit.launcher.config.StorageProperties;
import com.junit.launcher.model.ExecutionResponse;
import com.junit.launcher.model.ReportMetadata;
import com.junit.launcher.service.ArchiveService;
import com.junit.launcher.service.ReportService;
import com.junit.launcher.service.TestExecutionService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * REST controller for report operations.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {
    
    private final ReportService reportService;
    private final ArchiveService archiveService;
    private final StorageProperties storageProperties;
    private final TestExecutionService testExecutionService;
    
    public ReportController(ReportService reportService,
                           ArchiveService archiveService,
                           StorageProperties storageProperties,
                           TestExecutionService testExecutionService) {
        this.reportService = reportService;
        this.archiveService = archiveService;
        this.storageProperties = storageProperties;
        this.testExecutionService = testExecutionService;
    }
    
    /**
     * Lists all available reports.
     * 
     * @return List of report metadata sorted by timestamp (newest first)
     */
    @GetMapping
    public ResponseEntity<List<ReportMetadata>> listReports() {
        try {
            List<ReportMetadata> reports = reportService.listReports();
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Views a specific report by serving its index.html or any file within the report.
     * This handles paths like:
     * - /api/reports/{reportId}/index.html
     * - /api/reports/{reportId}/styles.css
     * - /api/reports/{reportId}/data/suites.json
     * 
     * @param reportId The report identifier
     * @param request The HTTP request to extract the file path
     * @return The requested file resource
     */
    @GetMapping("/{reportId}/**")
    public ResponseEntity<Resource> getReportFile(@PathVariable String reportId,
                                                   jakarta.servlet.http.HttpServletRequest request) {
        try {
            ReportMetadata metadata = reportService.getReportMetadata(reportId);
            if (metadata == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Extract the file path after /{reportId}/
            String requestUrl = request.getRequestURI();
            String prefix = "/api/reports/" + reportId + "/";
            String filePath = requestUrl.substring(requestUrl.indexOf(prefix) + prefix.length());
            
            // If no file specified, default to index.html
            if (filePath.isEmpty()) {
                filePath = "index.html";
            }
            
            Path fullPath = Paths.get(metadata.getReportPath(), filePath);
            
            // Security check: ensure the resolved path is within the report directory
            Path reportDir = Paths.get(metadata.getReportPath()).toAbsolutePath().normalize();
            Path requestedPath = fullPath.toAbsolutePath().normalize();
            
            if (!requestedPath.startsWith(reportDir)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            if (!Files.exists(fullPath) || Files.isDirectory(fullPath)) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(fullPath);
            
            // Determine content type based on file extension
            String contentType = determineContentType(filePath);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Downloads a report as a ZIP archive.
     * 
     * @param reportId The report identifier
     * @return ZIP file containing the report
     */
    @GetMapping("/{reportId}/download")
    public ResponseEntity<Resource> downloadReport(@PathVariable String reportId) {
        try {
            Path archivePath = archiveService.createReportArchive(reportId);
            if (archivePath == null || !Files.exists(archivePath)) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(archivePath);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "attachment; filename=\"" + archivePath.getFileName().toString() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Re-runs failed tests from a specific report.
     * 
     * @param reportId The report identifier
     * @return Execution response with new execution ID
     */
    @PostMapping("/{reportId}/rerun")
    public ResponseEntity<ExecutionResponse> rerunFailedTests(@PathVariable String reportId) {
        try {
            // Extract failed tests from the report
            List<String> failedTests = reportService.getFailedTests(reportId);
            
            if (failedTests.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            // Execute only the failed tests
            String executionId = testExecutionService.executeTests(failedTests);
            
            ExecutionResponse response = new ExecutionResponse();
            response.setExecutionId(executionId);
            response.setStatus("RUNNING");
            response.setTimestamp(java.time.LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    
    /**
     * Determines the content type based on file extension.
     */
    private String determineContentType(String filePath) {
        String lowerPath = filePath.toLowerCase();
        if (lowerPath.endsWith(".css")) {
            return "text/css";
        } else if (lowerPath.endsWith(".js")) {
            return "application/javascript";
        } else if (lowerPath.endsWith(".json")) {
            return "application/json";
        } else if (lowerPath.endsWith(".png")) {
            return "image/png";
        } else if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerPath.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (lowerPath.endsWith(".html")) {
            return "text/html";
        } else {
            return "application/octet-stream";
        }
    }
}
