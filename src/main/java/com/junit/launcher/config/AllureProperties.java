package com.junit.launcher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Allure CLI.
 */
@Component
@ConfigurationProperties(prefix = "allure.cli")
public class AllureProperties {

    private String path = "allure";

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
