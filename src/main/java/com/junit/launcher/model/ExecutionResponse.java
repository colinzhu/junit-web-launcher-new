package com.junit.launcher.model;

import java.util.Objects;

/**
 * Response containing execution tracking information.
 */
public class ExecutionResponse {
    private String executionId;
    private String status;
    private String timestamp;
    private String reportId;

    public ExecutionResponse() {
    }

    public ExecutionResponse(String executionId, String status, String timestamp) {
        this.executionId = executionId;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionResponse that = (ExecutionResponse) o;
        return Objects.equals(executionId, that.executionId) &&
               Objects.equals(status, that.status) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(reportId, that.reportId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(executionId, status, timestamp, reportId);
    }

    @Override
    public String toString() {
        return "ExecutionResponse{" +
               "executionId='" + executionId + '\'' +
               ", status='" + status + '\'' +
               ", timestamp='" + timestamp + '\'' +
               ", reportId='" + reportId + '\'' +
               '}';
    }
}
