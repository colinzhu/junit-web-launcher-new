package com.junit.launcher.service;

import com.junit.launcher.model.ExecutionStatus;

import java.util.List;

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
    
    /**
     * Gets the report ID for a completed execution.
     * 
     * @param executionId The execution to check
     * @return Report ID if available, null otherwise
     */
    String getReportId(String executionId);
}
