package com.junit.launcher.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Metadata about a generated report.
 */
public class ReportMetadata {
    private String reportId;
    private String executionId;
    private String timestamp;
    private int totalTests;
    private int passedTests;
    private int failedTests;
    private int skippedTests;
    private String reportPath;
    private boolean isCombined;
    private List<String> combinedReportIds;

    public ReportMetadata() {
        this.combinedReportIds = new ArrayList<>();
    }

    public ReportMetadata(String reportId, String executionId, String timestamp, 
                          int totalTests, int passedTests, int failedTests, int skippedTests,
                          String reportPath, boolean isCombined, List<String> combinedReportIds) {
        this.reportId = reportId;
        this.executionId = executionId;
        this.timestamp = timestamp;
        this.totalTests = totalTests;
        this.passedTests = passedTests;
        this.failedTests = failedTests;
        this.skippedTests = skippedTests;
        this.reportPath = reportPath;
        this.isCombined = isCombined;
        this.combinedReportIds = combinedReportIds != null ? new ArrayList<>(combinedReportIds) : new ArrayList<>();
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getTotalTests() {
        return totalTests;
    }

    public void setTotalTests(int totalTests) {
        this.totalTests = totalTests;
    }

    public int getPassedTests() {
        return passedTests;
    }

    public void setPassedTests(int passedTests) {
        this.passedTests = passedTests;
    }

    public int getFailedTests() {
        return failedTests;
    }

    public void setFailedTests(int failedTests) {
        this.failedTests = failedTests;
    }

    public int getSkippedTests() {
        return skippedTests;
    }

    public void setSkippedTests(int skippedTests) {
        this.skippedTests = skippedTests;
    }

    public String getReportPath() {
        return reportPath;
    }

    public void setReportPath(String reportPath) {
        this.reportPath = reportPath;
    }

    public boolean isCombined() {
        return isCombined;
    }

    public void setCombined(boolean combined) {
        isCombined = combined;
    }

    public List<String> getCombinedReportIds() {
        return combinedReportIds;
    }

    public void setCombinedReportIds(List<String> combinedReportIds) {
        this.combinedReportIds = combinedReportIds != null ? new ArrayList<>(combinedReportIds) : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReportMetadata that = (ReportMetadata) o;
        return totalTests == that.totalTests &&
               passedTests == that.passedTests &&
               failedTests == that.failedTests &&
               skippedTests == that.skippedTests &&
               isCombined == that.isCombined &&
               Objects.equals(reportId, that.reportId) &&
               Objects.equals(executionId, that.executionId) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(reportPath, that.reportPath) &&
               Objects.equals(combinedReportIds, that.combinedReportIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reportId, executionId, timestamp, totalTests, passedTests, 
                          failedTests, skippedTests, reportPath, isCombined, combinedReportIds);
    }

    @Override
    public String toString() {
        return "ReportMetadata{" +
               "reportId='" + reportId + '\'' +
               ", executionId='" + executionId + '\'' +
               ", timestamp='" + timestamp + '\'' +
               ", totalTests=" + totalTests +
               ", passedTests=" + passedTests +
               ", failedTests=" + failedTests +
               ", skippedTests=" + skippedTests +
               ", reportPath='" + reportPath + '\'' +
               ", isCombined=" + isCombined +
               ", combinedReportIds=" + combinedReportIds +
               '}';
    }
}
