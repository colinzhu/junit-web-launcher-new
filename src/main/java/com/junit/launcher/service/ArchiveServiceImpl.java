package com.junit.launcher.service;

import com.junit.launcher.config.StorageProperties;
import com.junit.launcher.model.LogFileMetadata;
import com.junit.launcher.model.ReportMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Implementation of ArchiveService for managing logs and reports.
 */
@Service
public class ArchiveServiceImpl implements ArchiveService {
    
    private static final Logger logger = LoggerFactory.getLogger(ArchiveServiceImpl.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final String LOG_FILE_PREFIX = "execution-";
    private static final String LOG_FILE_EXTENSION = ".log";
    
    private final StorageProperties storageProperties;
    private final ReportService reportService;

    public ArchiveServiceImpl(StorageProperties storageProperties, ReportService reportService) {
        this.storageProperties = storageProperties;
        this.reportService = reportService;
    }

    @Override
    public List<LogFileMetadata> listLogFiles() {
        Path logsPath = Paths.get(storageProperties.getLogsPath());

        if (!Files.exists(logsPath)) {
            return new ArrayList<>();
        }

        try (Stream<Path> paths = Files.list(logsPath)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(LOG_FILE_EXTENSION))
                .map(this::createLogFileMetadata)
                .filter(metadata -> metadata != null)
                .sorted(Comparator.comparing(LogFileMetadata::getTimestamp).reversed())
                .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Error listing log files", e);
            return new ArrayList<>();
        }
    }

    @Override
    public void archiveLogs(String executionId, String logs) throws Exception {
        logger.info("Archiving logs for execution: {}", executionId);

        // Create logs directory if it doesn't exist
        Path logsDir = Paths.get(storageProperties.getLogsPath());
        Files.createDirectories(logsDir);

        // Create log file with timestamp-based naming (add UUID suffix to ensure uniqueness)
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String logFileName = LOG_FILE_PREFIX + timestamp + "_" + uniqueSuffix + LOG_FILE_EXTENSION;
        Path logFile = logsDir.resolve(logFileName);

        // Write logs to file
        Files.writeString(logFile, logs);

        logger.info("Logs archived successfully: {}", logFileName);
    }

    @Override
    public String getLogFile(String logFileId) throws Exception {
        Path logFile = Paths.get(storageProperties.getLogsPath(), logFileId);

        if (!Files.exists(logFile)) {
            logger.warn("Log file not found: {}", logFileId);
            return null;
        }

        return Files.readString(logFile);
    }

    @Override
    public List<ReportMetadata> listReports() {
        // Delegate to ReportService which already implements this
        return reportService.listReports();
    }

    @Override
    public Path createReportArchive(String reportId) throws Exception {
        logger.info("Creating archive for report: {}", reportId);
        
        // Get report directory
        Path reportDir = Paths.get(storageProperties.getReportsPath(), reportId);
        
        if (!Files.exists(reportDir)) {
            throw new IllegalArgumentException("Report not found: " + reportId);
        }
        
        // Create ZIP file in the same directory
        Path zipFile = Paths.get(storageProperties.getReportsPath(), reportId + ".zip");
        
        // Create ZIP archive
        try (ZipOutputStream zos = new ZipOutputStream(
                new BufferedOutputStream(new FileOutputStream(zipFile.toFile())))) {
            
            // Add all files from report directory to ZIP
            zipDirectory(reportDir, reportDir, zos);
        }
        
        logger.info("Report archive created successfully: {}", zipFile.getFileName());
        return zipFile;
    }
    
    /**
     * Creates LogFileMetadata from a log file path.
     */
    private LogFileMetadata createLogFileMetadata(Path logFile) {
        try {
            String fileName = logFile.getFileName().toString();
            long fileSize = Files.size(logFile);
            
            // Extract timestamp from filename (execution-YYYY-MM-DD_HH-MM-SS.log)
            String timestamp = extractTimestampFromLogFileName(fileName);
            
            // Extract execution ID (use filename without extension as logId)
            String logId = fileName.replace(LOG_FILE_EXTENSION, "");
            String executionId = logId; // For now, use same as logId
            
            return new LogFileMetadata(
                fileName,
                executionId,
                timestamp,
                fileSize,
                logFile.toString()
            );
        } catch (IOException e) {
            logger.error("Failed to create metadata for log file: {}", logFile, e);
            return null;
        }
    }
    
    /**
     * Extracts timestamp from log file name.
     */
    private String extractTimestampFromLogFileName(String fileName) {
        // Expected format: execution-YYYY-MM-DD_HH-MM-SS_UUID.log
        if (fileName.startsWith(LOG_FILE_PREFIX) && fileName.endsWith(LOG_FILE_EXTENSION)) {
            int startIndex = LOG_FILE_PREFIX.length();
            int endIndex = fileName.length() - LOG_FILE_EXTENSION.length();
            String timestampWithUuid = fileName.substring(startIndex, endIndex);
            
            // Remove UUID suffix (last underscore and 8 characters)
            int lastUnderscore = timestampWithUuid.lastIndexOf('_');
            if (lastUnderscore > 0) {
                return timestampWithUuid.substring(0, lastUnderscore);
            }
            return timestampWithUuid;
        }
        return "";
    }
    
    /**
     * Recursively adds directory contents to ZIP archive.
     */
    private void zipDirectory(Path sourceDir, Path basePath, ZipOutputStream zos) throws IOException {
        try (Stream<Path> paths = Files.walk(sourceDir)) {
            List<Path> files = paths.filter(path -> !Files.isDirectory(path)).collect(Collectors.toList());
            for (Path path : files) {
                try {
                    // Get relative path for ZIP entry
                    Path relativePath = basePath.relativize(path);
                    String zipEntryName = relativePath.toString().replace("\\", "/");
                    
                    ZipEntry zipEntry = new ZipEntry(zipEntryName);
                    zos.putNextEntry(zipEntry);
                    
                    Files.copy(path, zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    logger.error("Failed to add file to ZIP: {}", path, e);
                    throw e; // Fail fast if a file cannot be added
                }
            }
        }
    }
}
