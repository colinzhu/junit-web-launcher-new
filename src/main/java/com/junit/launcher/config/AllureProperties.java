package com.junit.launcher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Allure.
 */
@Component
@ConfigurationProperties(prefix = "allure")
public class AllureProperties {

    public String getPath() {
        return "allure"; // Return default value directly since we're not using CLI
    }

    public void setPath(String path) {
        // No-op since we're not using CLI anymore
    }
}