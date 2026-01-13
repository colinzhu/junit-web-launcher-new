package com.junit.launcher.model;

import java.util.Objects;

/**
 * Metadata about an archived log file.
 */
public class LogFileMetadata {
    private String logId;
    private String executionId;
    private String timestamp;
    private long fileSizeBytes;
    private String filePath;

    public LogFileMetadata() {
    }

    public LogFileMetadata(String logId, String executionId, String timestamp, 
                           long fileSizeBytes, String filePath) {
        this.logId = logId;
        this.executionId = executionId;
        this.timestamp = timestamp;
        this.fileSizeBytes = fileSizeBytes;
        this.filePath = filePath;
    }

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
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

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogFileMetadata that = (LogFileMetadata) o;
        return fileSizeBytes == that.fileSizeBytes &&
               Objects.equals(logId, that.logId) &&
               Objects.equals(executionId, that.executionId) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(filePath, that.filePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logId, executionId, timestamp, fileSizeBytes, filePath);
    }

    @Override
    public String toString() {
        return "LogFileMetadata{" +
               "logId='" + logId + '\'' +
               ", executionId='" + executionId + '\'' +
               ", timestamp='" + timestamp + '\'' +
               ", fileSizeBytes=" + fileSizeBytes +
               ", filePath='" + filePath + '\'' +
               '}';
    }
}
