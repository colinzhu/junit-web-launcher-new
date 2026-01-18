package com.junit.launcher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for file storage paths.
 */
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    private String reportsPath = "./allure-report";
    private String logsPath = "./storage/logs";
    private String allureResultsPath = "./allure-results";

    public String getReportsPath() {
        return reportsPath;
    }

    public void setReportsPath(String reportsPath) {
        this.reportsPath = reportsPath;
    }

    public String getLogsPath() {
        return logsPath;
    }

    public void setLogsPath(String logsPath) {
        this.logsPath = logsPath;
    }

    public String getAllureResultsPath() {
        return allureResultsPath;
    }

    public void setAllureResultsPath(String allureResultsPath) {
        this.allureResultsPath = allureResultsPath;
    }
}