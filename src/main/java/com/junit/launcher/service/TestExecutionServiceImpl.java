package com.junit.launcher.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.platform.engine.TestExecutionResult;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.junit.launcher.model.ExecutionStatus;
import com.junit.launcher.model.ReportMetadata;

/**
 * Implementation of TestExecutionService for executing JUnit tests.
 */
@Service
public class TestExecutionServiceImpl implements TestExecutionService {
    
    private static final Logger logger = LoggerFactory.getLogger(TestExecutionServiceImpl.class);
    
    private final Map<String, ExecutionContext> activeExecutions = new ConcurrentHashMap<>();
    private final LogStreamingService logStreamingService;
    private final AllureConfigurationService allureConfigurationService;
    private final ReportService reportService;
    
    public TestExecutionServiceImpl(LogStreamingService logStreamingService, 
                                   AllureConfigurationService allureConfigurationService,
                                   ReportService reportService) {
        this.logStreamingService = logStreamingService;
        this.allureConfigurationService = allureConfigurationService;
        this.reportService = reportService;
    }
    
    @Override
    public String executeTests(List<String> selectedTests) {
        if (selectedTests == null || selectedTests.isEmpty()) {
            throw new IllegalArgumentException("Selected tests cannot be null or empty");
        }
        
        // Generate unique execution ID
        String executionId = UUID.randomUUID().toString();
        
        logger.info("Starting test execution with ID: {}", executionId);
        
        // Create execution context
        ExecutionContext context = new ExecutionContext(executionId);
        activeExecutions.put(executionId, context);
        
        // Start execution in background thread
        Thread executionThread = new Thread(() -> runTests(executionId, selectedTests, context));
        executionThread.setName("test-execution-" + executionId);
        context.setExecutionThread(executionThread);
        executionThread.start();
        
        return executionId;
    }
    
    @Override
    public void cancelExecution(String executionId) {
        ExecutionContext context = activeExecutions.get(executionId);
        if (context == null) {
            logger.warn("Execution not found: {}", executionId);
            return;
        }
        
        logger.info("Cancelling execution: {}", executionId);
        context.setStatus(ExecutionStatus.CANCELLED);
        
        Thread thread = context.getExecutionThread();
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }
    
    @Override
    public ExecutionStatus getExecutionStatus(String executionId) {
        ExecutionContext context = activeExecutions.get(executionId);
        if (context == null) {
            return null;
        }
        return context.getStatus();
    }
    
    @Override
    public String getReportId(String executionId) {
        ExecutionContext context = activeExecutions.get(executionId);
        if (context == null) {
            return null;
        }
        return context.getReportId();
    }
    
    /**
     * Runs the selected tests.
     */
    private void runTests(String executionId, List<String> selectedTests, ExecutionContext context) {
        try {
            // Send initial log message
            logStreamingService.publishLog(executionId, "=== Test Execution Started ===\n");
            logStreamingService.publishLog(executionId, String.format("Execution ID: %s%n", executionId));
            logStreamingService.publishLog(executionId, String.format("Selected tests: %d%n", selectedTests.size()));
            logStreamingService.publishLog(executionId, "==============================\n");
            
            // Configure Allure for this execution
            allureConfigurationService.configureAllureForExecution(executionId);
            
            // Redirect stdout/stderr to capture logs
            if (logStreamingService instanceof LogStreamingServiceImpl) {
                ((LogStreamingServiceImpl) logStreamingService).redirectStreams(executionId);
            }
            
            // Build discovery request with selected test IDs
            LauncherDiscoveryRequestBuilder requestBuilder = LauncherDiscoveryRequestBuilder.request();
            for (String testId : selectedTests) {
                requestBuilder.selectors(selectUniqueId(testId));
            }
            LauncherDiscoveryRequest request = requestBuilder.build();
            
            // Create launcher and register listener
            Launcher launcher = LauncherFactory.create();
            CustomTestExecutionListener listener = new CustomTestExecutionListener(executionId, context, logStreamingService);
            launcher.registerTestExecutionListeners(listener);
            
            // Execute tests
            logger.info("Executing {} tests for execution ID: {}", selectedTests.size(), executionId);
            launcher.execute(request);
            
            // Check if execution was cancelled
            if (context.getStatus() == ExecutionStatus.CANCELLED) {
                logger.info("Execution cancelled: {}", executionId);
                logStreamingService.publishLog(executionId, "\n=== Execution Cancelled ===\n");
            } else {
                context.setStatus(ExecutionStatus.COMPLETED);
                logger.info("Execution completed: {}", executionId);
                logStreamingService.publishLog(executionId, "\n=== Execution Completed ===\n");
                
                // Auto-generate Allure report
                try {
                    logStreamingService.publishLog(executionId, "\n=== Generating Allure Report ===\n");
                    ReportMetadata reportMetadata = reportService.generateReport(executionId);
                    context.setReportId(reportMetadata.getReportId());
                    logStreamingService.publishLog(executionId, String.format("Report generated: %s%n", reportMetadata.getReportId()));
                    logger.info("Report generated for execution {}: {}", executionId, reportMetadata.getReportId());
                } catch (Exception e) {
                    logger.error("Failed to generate report for execution: {}", executionId, e);
                    logStreamingService.publishLog(executionId, String.format("Warning: Failed to generate report: %s%n", e.getMessage()));
                }
            }
            
        } catch (Exception e) {
            if (Thread.currentThread().isInterrupted() || context.getStatus() == ExecutionStatus.CANCELLED) {
                logger.info("Execution interrupted: {}", executionId);
                context.setStatus(ExecutionStatus.CANCELLED);
                logStreamingService.publishLog(executionId, "\n=== Execution Interrupted ===\n");
                Thread.currentThread().interrupt();
            } else {
                logger.error("Execution failed: {}", executionId, e);
                context.setStatus(ExecutionStatus.FAILED);
                logStreamingService.publishLog(executionId, String.format("%n=== Execution Failed: %s ===%n", e.getMessage()));
            }
        } finally {
            // Cleanup Allure configuration
            allureConfigurationService.cleanupAllureConfiguration(executionId);
            
            // Restore original streams
            if (logStreamingService instanceof LogStreamingServiceImpl) {
                ((LogStreamingServiceImpl) logStreamingService).restoreStreams();
            }
            
            // Complete streaming
            logStreamingService.completeStreaming(executionId);
        }
    }
    
    /**
     * Context for tracking execution state.
     */
    private static class ExecutionContext {
        private final String executionId;
        private volatile ExecutionStatus status;
        private Thread executionThread;
        private volatile String reportId;
        
        public ExecutionContext(String executionId) {
            this.executionId = executionId;
            this.status = ExecutionStatus.RUNNING;
        }
        
        public String getExecutionId() {
            return executionId;
        }
        
        public ExecutionStatus getStatus() {
            return status;
        }
        
        public void setStatus(ExecutionStatus status) {
            this.status = status;
        }
        
        public Thread getExecutionThread() {
            return executionThread;
        }
        
        public void setExecutionThread(Thread executionThread) {
            this.executionThread = executionThread;
        }
        
        public String getReportId() {
            return reportId;
        }
        
        public void setReportId(String reportId) {
            this.reportId = reportId;
        }
    }
    
    /**
     * Custom listener to capture test execution events.
     */
    private static class CustomTestExecutionListener implements TestExecutionListener {
        private final String executionId;
        private final ExecutionContext context;
        private final LogStreamingService logStreamingService;
        
        public CustomTestExecutionListener(String executionId, ExecutionContext context, LogStreamingService logStreamingService) {
            this.executionId = executionId;
            this.context = context;
            this.logStreamingService = logStreamingService;
        }
        
        @Override
        public void executionStarted(TestIdentifier testIdentifier) {
            // Check for cancellation
            if (Thread.currentThread().isInterrupted() || context.getStatus() == ExecutionStatus.CANCELLED) {
                throw new RuntimeException("Execution cancelled");
            }
            
            if (testIdentifier.isTest()) {
                String message = String.format("[TEST STARTED] %s%n", testIdentifier.getDisplayName());
                System.out.print(message);
                System.out.flush();
                // Also publish directly to ensure it's sent
                logStreamingService.publishLog(executionId, message);
                logger.debug("Test started: {} [{}]", testIdentifier.getDisplayName(), executionId);
            }
        }
        
        @Override
        public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
            if (testIdentifier.isTest()) {
                String status = testExecutionResult.getStatus().name();
                String message = String.format("[TEST FINISHED] %s - Status: %s%n", 
                    testIdentifier.getDisplayName(), status);
                System.out.print(message);
                System.out.flush();
                // Also publish directly to ensure it's sent
                logStreamingService.publishLog(executionId, message);
                logger.debug("Test finished: {} - Status: {} [{}]", 
                    testIdentifier.getDisplayName(), status, executionId);
            }
        }
        
        @Override
        public void executionSkipped(TestIdentifier testIdentifier, String reason) {
            if (testIdentifier.isTest()) {
                String message = String.format("[TEST SKIPPED] %s - Reason: %s%n", 
                    testIdentifier.getDisplayName(), reason);
                System.out.print(message);
                System.out.flush();
                // Also publish directly to ensure it's sent
                logStreamingService.publishLog(executionId, message);
                logger.debug("Test skipped: {} - Reason: {} [{}]", 
                    testIdentifier.getDisplayName(), reason, executionId);
            }
        }
    }
}
