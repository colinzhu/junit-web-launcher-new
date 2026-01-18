package com.junit.launcher.service;

import com.junit.launcher.config.StorageProperties;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.FileSystemResultsWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
     * Creates a thread-safe Allure lifecycle for a specific test execution.
     *
     * @param executionId The unique execution identifier
     * @return AllureLifecycle instance for this execution
     * @throws IOException if directory creation fails
     */
    public AllureLifecycle createLifecycleForExecution(String executionId) throws IOException {
        Path resultsDir = getResultsDirectory(executionId);
        Files.createDirectories(resultsDir);

        logger.info("Created Allure results directory for execution {}: {}", executionId, resultsDir);

        FileSystemResultsWriter writer = new FileSystemResultsWriter(resultsDir);
        return new AllureLifecycle(writer);
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
