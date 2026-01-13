package com.junit.launcher.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.junit.launcher.config.StorageProperties;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;

/**
 * Service for configuring Allure lifecycle for test executions.
 */
@Service
public class AllureConfigurationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AllureConfigurationService.class);
    
    private final StorageProperties storageProperties;
    
    public AllureConfigurationService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }
    
    /**
     * Configures Allure for a specific test execution.
     * Creates a dedicated results directory and sets up the Allure lifecycle.
     * 
     * @param executionId The unique execution identifier
     * @return Path to the Allure results directory
     * @throws IOException if directory creation fails
     */
    public Path configureAllureForExecution(String executionId) throws IOException {
        // Create execution-specific results directory
        Path resultsDir = Paths.get(storageProperties.getAllureResultsPath(), executionId);
        Files.createDirectories(resultsDir);
        
        logger.info("Created Allure results directory for execution {}: {}", executionId, resultsDir);
        
        // Set system property for Allure results directory
        System.setProperty("allure.results.directory", resultsDir.toAbsolutePath().toString());
        
        // Get or create Allure lifecycle instance
        AllureLifecycle lifecycle = Allure.getLifecycle();
        logger.debug("Allure lifecycle configured for execution: {}", executionId);
        
        return resultsDir;
    }
    
    /**
     * Cleans up Allure configuration after test execution.
     * 
     * @param executionId The execution identifier
     */
    public void cleanupAllureConfiguration(String executionId) {
        // Clear the system property
        System.clearProperty("allure.results.directory");
        logger.debug("Cleaned up Allure configuration for execution: {}", executionId);
    }
    
    /**
     * Gets the results directory path for a specific execution.
     * 
     * @param executionId The execution identifier
     * @return Path to the results directory
     */
    public Path getResultsDirectory(String executionId) {
        return Paths.get(storageProperties.getAllureResultsPath(), executionId);
    }
}
