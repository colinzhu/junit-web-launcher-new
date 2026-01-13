package com.junit.launcher.service;

import java.util.List;

import com.junit.launcher.model.ReportMetadata;

/**
 * Service for generating and managing Allure test reports.
 */
public interface ReportService {
    
    /**
     * Generates an Allure report from execution results.
     * 
     * @param executionId The execution to generate report for
     * @return Report metadata including path and timestamp
     * @throws Exception if report generation fails
     */
    ReportMetadata generateReport(String executionId) throws Exception;
    
    /**
     * Lists all available reports.
     * 
     * @return List of report metadata sorted by timestamp (newest first)
     */
    List<ReportMetadata> listReports();
    
    /**
     * Combines multiple reports into one.
     * 
     * @param reportIds List of report IDs to combine
     * @return Combined report metadata
     * @throws Exception if report combination fails
     */
    ReportMetadata combineReports(List<String> reportIds) throws Exception;
    
    /**
     * Gets metadata for a specific report.
     * 
     * @param reportId The report identifier
     * @return Report metadata or null if not found
     */
    ReportMetadata getReportMetadata(String reportId);
}
