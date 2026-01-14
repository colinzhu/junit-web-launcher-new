package com.junit.launcher.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.junit.launcher.config.AllureProperties;
import com.junit.launcher.config.StorageProperties;
import com.junit.launcher.model.ReportMetadata;

/**
 * Implementation of ReportService for generating Allure reports.
 */
@Service
public class ReportServiceImpl implements ReportService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportServiceImpl.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final String METADATA_FILE = "metadata.json";
    
    private final AllureProperties allureProperties;
    private final StorageProperties storageProperties;
    private final AllureConfigurationService allureConfigurationService;
    private final ObjectMapper objectMapper;
    
    public ReportServiceImpl(AllureProperties allureProperties,
                            StorageProperties storageProperties,
                            AllureConfigurationService allureConfigurationService) {
        this.allureProperties = allureProperties;
        this.storageProperties = storageProperties;
        this.allureConfigurationService = allureConfigurationService;
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public ReportMetadata generateReport(String executionId) throws Exception {
        logger.info("Generating Allure report for execution: {}", executionId);
        
        // Get results directory
        Path resultsDir = allureConfigurationService.getResultsDirectory(executionId);
        if (!Files.exists(resultsDir)) {
            throw new IllegalArgumentException("Results directory not found for execution: " + executionId);
        }
        
        // Copy history from previous report to current results directory
        copyHistoryFromPreviousReport(resultsDir);
        
        // Create report directory with timestamp
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String reportId = "allure-report-" + timestamp;
        Path reportDir = Paths.get(storageProperties.getReportsPath(), reportId);
        Files.createDirectories(reportDir);
        
        // Execute Allure CLI to generate report
        executeAllureGenerate(resultsDir, reportDir);
        
        // Parse test results to create metadata
        ReportMetadata metadata = createReportMetadata(reportId, executionId, timestamp, resultsDir);
        
        // Save metadata to JSON file
        saveMetadata(reportDir, metadata);
        
        logger.info("Report generated successfully: {}", reportId);
        return metadata;
    }
    
    @Override
    public List<ReportMetadata> listReports() {
        Path reportsPath = Paths.get(storageProperties.getReportsPath());
        
        if (!Files.exists(reportsPath)) {
            return new ArrayList<>();
        }
        
        try (Stream<Path> paths = Files.list(reportsPath)) {
            return paths
                .filter(Files::isDirectory)
                .map(this::loadMetadata)
                .filter(metadata -> metadata != null)
                .sorted(Comparator.comparing(ReportMetadata::getTimestamp).reversed())
                .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Error listing reports", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public ReportMetadata combineReports(List<String> reportIds) throws Exception {
        logger.info("Combining {} reports", reportIds.size());
        
        if (reportIds == null || reportIds.size() < 2) {
            throw new IllegalArgumentException("At least 2 reports are required for combination");
        }
        
        // Create temporary combined results directory
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String combinedExecutionId = "combined-" + timestamp;
        Path combinedResultsDir = Paths.get(storageProperties.getAllureResultsPath(), combinedExecutionId);
        Files.createDirectories(combinedResultsDir);
        
        // Copy all allure-results from selected reports
        for (String reportId : reportIds) {
            ReportMetadata metadata = getReportMetadata(reportId);
            if (metadata == null) {
                throw new IllegalArgumentException("Report not found: " + reportId);
            }
            
            Path sourceResultsDir = allureConfigurationService.getResultsDirectory(metadata.getExecutionId());
            if (Files.exists(sourceResultsDir)) {
                copyAllureResults(sourceResultsDir, combinedResultsDir);
            }
        }
        
        // Copy history from the most recent report among the selected reports
        copyHistoryFromSelectedReports(reportIds, combinedResultsDir);
        
        // Generate combined report
        String combinedReportId = "allure-report-combined-" + timestamp;
        Path combinedReportDir = Paths.get(storageProperties.getReportsPath(), combinedReportId);
        Files.createDirectories(combinedReportDir);
        
        executeAllureGenerate(combinedResultsDir, combinedReportDir);
        
        // Create metadata for combined report
        ReportMetadata metadata = createReportMetadata(combinedReportId, combinedExecutionId, timestamp, combinedResultsDir);
        metadata.setCombined(true);
        metadata.setCombinedReportIds(reportIds);
        
        // Save metadata
        saveMetadata(combinedReportDir, metadata);
        
        logger.info("Combined report generated successfully: {}", combinedReportId);
        return metadata;
    }
    
    @Override
    public ReportMetadata getReportMetadata(String reportId) {
        Path reportDir = Paths.get(storageProperties.getReportsPath(), reportId);
        return loadMetadata(reportDir);
    }
    
    @Override
    public List<String> getFailedTests(String reportId) throws Exception {
        logger.info("Extracting failed tests from report: {}", reportId);
        
        ReportMetadata metadata = getReportMetadata(reportId);
        if (metadata == null) {
            throw new IllegalArgumentException("Report not found: " + reportId);
        }
        
        // Get the results directory for this execution
        Path resultsDir = allureConfigurationService.getResultsDirectory(metadata.getExecutionId());
        if (!Files.exists(resultsDir)) {
            throw new IllegalArgumentException("Results directory not found for execution: " + metadata.getExecutionId());
        }
        
        List<String> failedTestIds = new ArrayList<>();
        
        // Parse all result files to find failed tests
        try (Stream<Path> files = Files.list(resultsDir)) {
            List<Path> resultFiles = files
                .filter(p -> p.getFileName().toString().endsWith("-result.json"))
                .collect(Collectors.toList());
            
            for (Path resultFile : resultFiles) {
                try {
                    String content = Files.readString(resultFile);
                    
                    // Parse JSON to extract test information
                    @SuppressWarnings("unchecked")
                    Map<String, Object> testResult = objectMapper.readValue(content, Map.class);
                    
                    String status = (String) testResult.get("status");
                    if ("failed".equals(status)) {
                        String testCaseId = (String) testResult.get("testCaseId");
                        if (testCaseId != null) {
                            failedTestIds.add(testCaseId);
                        }
                    }
                } catch (IOException e) {
                    logger.warn("Failed to parse result file: {}", resultFile, e);
                }
            }
        }
        
        logger.info("Found {} failed tests in report: {}", failedTestIds.size(), reportId);
        return failedTestIds;
    }
    
    /**
     * Executes Allure CLI to generate report.
     */
    private void executeAllureGenerate(Path resultsDir, Path reportDir) throws Exception {
        String allureCommand = allureProperties.getPath();
        
        ProcessBuilder processBuilder = new ProcessBuilder(
            allureCommand,
            "generate",
            resultsDir.toAbsolutePath().toString(),
            "-o",
            reportDir.toAbsolutePath().toString()
        );
        
        processBuilder.redirectErrorStream(true);
        
        logger.debug("Executing Allure command: {}", String.join(" ", processBuilder.command()));
        
        try {
            Process process = processBuilder.start();
            
            // Capture output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    logger.debug("Allure: {}", line);
                }
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                String errorMessage = "Allure report generation failed with exit code " + exitCode + 
                                    ". Output: " + output.toString();
                logger.error(errorMessage);
                throw new RuntimeException(errorMessage);
            }
            
            logger.info("Allure report generated successfully");
            
        } catch (IOException e) {
            String errorMessage = "Failed to execute Allure CLI. Make sure Allure is installed and accessible. " +
                                "Command: " + allureCommand;
            logger.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }
    
    /**
     * Creates report metadata by parsing test results.
     */
    private ReportMetadata createReportMetadata(String reportId, String executionId, 
                                               String timestamp, Path resultsDir) throws IOException {
        ReportMetadata metadata = new ReportMetadata();
        metadata.setReportId(reportId);
        metadata.setExecutionId(executionId);
        metadata.setTimestamp(timestamp);
        metadata.setReportPath(Paths.get(storageProperties.getReportsPath(), reportId).toString());
        
        // Parse test results from allure-results directory
        // Count test result files
        int totalTests = 0;
        int passedTests = 0;
        int failedTests = 0;
        int skippedTests = 0;
        
        if (Files.exists(resultsDir)) {
            try (Stream<Path> files = Files.list(resultsDir)) {
                List<Path> resultFiles = files
                    .filter(p -> p.getFileName().toString().endsWith("-result.json"))
                    .collect(Collectors.toList());
                
                totalTests = resultFiles.size();
                
                // Parse each result file to get status
                for (Path resultFile : resultFiles) {
                    try {
                        String content = Files.readString(resultFile);
                        if (content.contains("\"status\":\"passed\"") || content.contains("\"status\" : \"passed\"")) {
                            passedTests++;
                        } else if (content.contains("\"status\":\"failed\"") || content.contains("\"status\" : \"failed\"")) {
                            failedTests++;
                        } else if (content.contains("\"status\":\"skipped\"") || content.contains("\"status\" : \"skipped\"")) {
                            skippedTests++;
                        }
                    } catch (IOException e) {
                        logger.warn("Failed to parse result file: {}", resultFile, e);
                    }
                }
            }
        }
        
        metadata.setTotalTests(totalTests);
        metadata.setPassedTests(passedTests);
        metadata.setFailedTests(failedTests);
        metadata.setSkippedTests(skippedTests);
        
        return metadata;
    }
    
    /**
     * Saves metadata to JSON file.
     */
    private void saveMetadata(Path reportDir, ReportMetadata metadata) throws IOException {
        Path metadataFile = reportDir.resolve(METADATA_FILE);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataFile.toFile(), metadata);
        logger.debug("Metadata saved to: {}", metadataFile);
    }
    
    /**
     * Loads metadata from report directory.
     */
    private ReportMetadata loadMetadata(Path reportDir) {
        Path metadataFile = reportDir.resolve(METADATA_FILE);
        
        if (!Files.exists(metadataFile)) {
            logger.warn("Metadata file not found: {}", metadataFile);
            return null;
        }
        
        try {
            return objectMapper.readValue(metadataFile.toFile(), ReportMetadata.class);
        } catch (IOException e) {
            logger.error("Failed to load metadata from: {}", metadataFile, e);
            return null;
        }
    }
    
    /**
     * Copies Allure result files from source to destination.
     */
    private void copyAllureResults(Path sourceDir, Path destDir) throws IOException {
        if (!Files.exists(sourceDir)) {
            return;
        }
        
        try (Stream<Path> files = Files.list(sourceDir)) {
            files.forEach(source -> {
                try {
                    Path dest = destDir.resolve(source.getFileName());
                    Files.copy(source, dest);
                } catch (IOException e) {
                    logger.warn("Failed to copy file: {}", source, e);
                }
            });
        }
    }
    
    /**
     * Copies history folder from the most recent report to the current results directory.
     * This enables Allure trend charts and historical comparison features.
     */
    private void copyHistoryFromPreviousReport(Path currentResultsDir) {
        try {
            // Get the most recent report
            List<ReportMetadata> reports = listReports();
            if (reports.isEmpty()) {
                logger.debug("No previous reports found, skipping history copy");
                return;
            }
            
            // Get the most recent report (reports are sorted by timestamp descending)
            ReportMetadata mostRecentReport = reports.get(0);
            Path mostRecentReportDir = Paths.get(storageProperties.getReportsPath(), mostRecentReport.getReportId());
            Path historyDir = mostRecentReportDir.resolve("history");
            
            if (!Files.exists(historyDir)) {
                logger.debug("No history folder found in most recent report: {}", mostRecentReport.getReportId());
                return;
            }
            
            // Copy history folder to current results directory
            Path targetHistoryDir = currentResultsDir.resolve("history");
            Files.createDirectories(targetHistoryDir);
            
            copyDirectoryRecursively(historyDir, targetHistoryDir);
            
            logger.info("Copied history from report {} to current results directory", mostRecentReport.getReportId());
            
        } catch (Exception e) {
            // Don't fail report generation if history copy fails
            logger.warn("Failed to copy history from previous report", e);
        }
    }
    
    /**
     * Recursively copies a directory and all its contents.
     */
    private void copyDirectoryRecursively(Path source, Path target) throws IOException {
        Files.walk(source)
            .forEach(sourcePath -> {
                try {
                    Path targetPath = target.resolve(source.relativize(sourcePath));
                    if (Files.isDirectory(sourcePath)) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.copy(sourcePath, targetPath);
                    }
                } catch (IOException e) {
                    logger.warn("Failed to copy path: {} to {}", sourcePath, target, e);
                }
            });
    }
    
    /**
     * Copies history folder from the most recent report among the selected reports.
     */
    private void copyHistoryFromSelectedReports(List<String> reportIds, Path combinedResultsDir) {
        try {
            // Find the most recent report among the selected ones
            ReportMetadata mostRecentReport = null;
            String mostRecentTimestamp = null;
            
            for (String reportId : reportIds) {
                ReportMetadata metadata = getReportMetadata(reportId);
                if (metadata != null) {
                    if (mostRecentTimestamp == null || metadata.getTimestamp().compareTo(mostRecentTimestamp) > 0) {
                        mostRecentReport = metadata;
                        mostRecentTimestamp = metadata.getTimestamp();
                    }
                }
            }
            
            if (mostRecentReport == null) {
                logger.debug("No valid reports found among selected reports for history copy");
                return;
            }
            
            Path mostRecentReportDir = Paths.get(storageProperties.getReportsPath(), mostRecentReport.getReportId());
            Path historyDir = mostRecentReportDir.resolve("history");
            
            if (!Files.exists(historyDir)) {
                logger.debug("No history folder found in most recent selected report: {}", mostRecentReport.getReportId());
                return;
            }
            
            // Copy history folder to combined results directory
            Path targetHistoryDir = combinedResultsDir.resolve("history");
            Files.createDirectories(targetHistoryDir);
            
            copyDirectoryRecursively(historyDir, targetHistoryDir);
            
            logger.info("Copied history from report {} to combined results directory", mostRecentReport.getReportId());
            
        } catch (Exception e) {
            // Don't fail report generation if history copy fails
            logger.warn("Failed to copy history from selected reports", e);
        }
    }
}
