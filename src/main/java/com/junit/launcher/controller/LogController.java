package com.junit.launcher.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.junit.launcher.model.LogFileMetadata;
import com.junit.launcher.service.ArchiveService;

/**
 * REST controller for log file operations.
 */
@RestController
@RequestMapping("/api/logs")
public class LogController {
    
    private final ArchiveService archiveService;
    
    public LogController(ArchiveService archiveService) {
        this.archiveService = archiveService;
    }
    
    /**
     * Lists all archived log files.
     * 
     * @return List of log file metadata sorted by timestamp (newest first)
     */
    @GetMapping
    public ResponseEntity<List<LogFileMetadata>> listLogs() {
        try {
            List<LogFileMetadata> logs = archiveService.listLogFiles();
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Views a specific log file content.
     * 
     * @param logId The log file identifier
     * @return Log file content as plain text
     */
    @GetMapping("/{logId}")
    public ResponseEntity<String> getLog(@PathVariable String logId) {
        try {
            String logContent = archiveService.getLogFile(logId);
            if (logContent == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(logContent);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Downloads a log file.
     * 
     * @param logId The log file identifier
     * @return Log file as downloadable content
     */
    @GetMapping("/{logId}/download")
    public ResponseEntity<Resource> downloadLog(@PathVariable String logId) {
        try {
            // Get log file metadata to find the file path
            List<LogFileMetadata> allLogs = archiveService.listLogFiles();
            LogFileMetadata targetLog = allLogs.stream()
                    .filter(log -> log.getLogId().equals(logId))
                    .findFirst()
                    .orElse(null);
            
            if (targetLog == null) {
                return ResponseEntity.notFound().build();
            }
            
            Path logPath = Paths.get(targetLog.getFilePath());
            if (!Files.exists(logPath)) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(logPath);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "attachment; filename=\"" + logPath.getFileName().toString() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
