package com.junit.launcher.service;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.junit.launcher.config.StorageProperties;
import com.junit.launcher.model.ExecutionStatus;

/**
 * Unit tests for TestExecutionServiceImpl.
 */
class TestExecutionServiceImplTest {
    
    private TestExecutionService executionService;
    private LogStreamingService logStreamingService;
    private AllureConfigurationService allureConfigurationService;
    
    @BeforeEach
    void setUp() {
        StorageProperties storageProperties = new StorageProperties();
        logStreamingService = new LogStreamingServiceImpl();
        allureConfigurationService = new AllureConfigurationService(storageProperties);
        executionService = new TestExecutionServiceImpl(logStreamingService, allureConfigurationService);
    }
    
    @Test
    void testExecuteTests_generatesExecutionId() {
        // Use a valid test ID from our test suite
        List<String> testIds = Arrays.asList(
            "[engine:junit-jupiter]/[class:com.junit.launcher.service.SampleTest]/[method:testAddition()]"
        );
        
        String executionId = executionService.executeTests(testIds);
        
        assertNotNull(executionId, "Execution ID should not be null");
        assertTrue(executionId.contains("_"), "Execution ID should contain timestamp separator");
    }
    
    @Test
    void testExecuteTests_setsInitialStatusToRunning() throws InterruptedException {
        List<String> testIds = Arrays.asList(
            "[engine:junit-jupiter]/[class:com.junit.launcher.service.SampleTest]/[method:testAddition()]"
        );
        
        String executionId = executionService.executeTests(testIds);
        
        // Check status immediately after starting
        ExecutionStatus status = executionService.getExecutionStatus(executionId);
        assertNotNull(status, "Status should not be null");
        assertTrue(status == ExecutionStatus.RUNNING || status == ExecutionStatus.COMPLETED,
            "Status should be RUNNING or COMPLETED");
    }
    
    @Test
    void testExecuteTests_throwsExceptionForNullTests() {
        assertThrows(IllegalArgumentException.class, () -> {
            executionService.executeTests(null);
        });
    }
    
    @Test
    void testExecuteTests_throwsExceptionForEmptyTests() {
        assertThrows(IllegalArgumentException.class, () -> {
            executionService.executeTests(Arrays.asList());
        });
    }
    
    @Test
    void testCancelExecution_updateStatusToCancelled() throws InterruptedException {
        // Start a long-running test (we'll use a simple test but cancel it quickly)
        List<String> testIds = Arrays.asList(
            "[engine:junit-jupiter]/[class:com.junit.launcher.service.SampleTest]/[method:testAddition()]"
        );
        
        String executionId = executionService.executeTests(testIds);
        
        // Cancel immediately
        executionService.cancelExecution(executionId);
        
        // Give it a moment to process cancellation
        Thread.sleep(100);
        
        ExecutionStatus status = executionService.getExecutionStatus(executionId);
        assertTrue(status == ExecutionStatus.CANCELLED || status == ExecutionStatus.COMPLETED,
            "Status should be CANCELLED or COMPLETED after cancellation");
    }
    
    @Test
    void testGetExecutionStatus_returnsNullForUnknownId() {
        ExecutionStatus status = executionService.getExecutionStatus("unknown-id");
        assertNull(status, "Status should be null for unknown execution ID");
    }
    
    @Test
    void testCancelExecution_handlesUnknownIdGracefully() {
        // Should not throw exception
        assertDoesNotThrow(() -> {
            executionService.cancelExecution("unknown-id");
        });
    }
}
