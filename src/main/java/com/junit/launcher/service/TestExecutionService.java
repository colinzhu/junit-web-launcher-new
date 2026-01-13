package com.junit.launcher.service;

import java.util.List;

import com.junit.launcher.model.ExecutionStatus;

/**
 * Service for executing JUnit tests.
 */
public interface TestExecutionService {
    
    /**
     * Executes selected tests and returns execution ID.
     * 
     * @param selectedTests List of test unique IDs to execute
     * @return Execution ID for tracking
     */
    String executeTests(List<String> selectedTests);
    
    /**
     * Cancels a running test execution.
     * 
     * @param executionId The execution to cancel
     */
    void cancelExecution(String executionId);
    
    /**
     * Gets the current status of an execution.
     * 
     * @param executionId The execution to check
     * @return Current execution status
     */
    ExecutionStatus getExecutionStatus(String executionId);
}
