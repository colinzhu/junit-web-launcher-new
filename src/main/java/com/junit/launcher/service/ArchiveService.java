package com.junit.launcher.service;

import java.nio.file.Path;
import java.util.List;

import com.junit.launcher.model.LogFileMetadata;
import com.junit.launcher.model.ReportMetadata;

/**
 * Service for managing storage and retrieval of historical logs and reports.
 */
public interface ArchiveService {
    
    /**
     * Saves execution logs to archive.
     * 
     * @param executionId The execution ID
     * @param logs The log content
     * @throws Exception if archiving fails
     */
    void archiveLogs(String executionId, String logs) throws Exception;
    
    /**
     * Lists all archived log files.
     * 
     * @return List of log file metadata sorted by timestamp (newest first)
     */
    List<LogFileMetadata> listLogFiles();
    
    /**
     * Retrieves log file content.
     * 
     * @param logFileId The log file to retrieve
     * @return Log file content or null if not found
     * @throws Exception if reading fails
     */
    String getLogFile(String logFileId) throws Exception;
    
    /**
     * Lists all available reports.
     * 
     * @return List of report metadata sorted by timestamp (newest first)
     */
    List<ReportMetadata> listReports();
    
    /**
     * Creates a downloadable archive of a report.
     * 
     * @param reportId The report to archive
     * @return Path to ZIP file
     * @throws Exception if archiving fails
     */
    Path createReportArchive(String reportId) throws Exception;
}
