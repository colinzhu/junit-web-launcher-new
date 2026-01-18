package com.junit.launcher.service;

import com.junit.launcher.model.ExecutionStatus;
import com.junit.launcher.model.ReportMetadata;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Status;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;

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
    private final ThreadPoolTaskExecutor taskExecutor;
    
    public TestExecutionServiceImpl(LogStreamingService logStreamingService, 
                                   AllureConfigurationService allureConfigurationService,
                                   ReportService reportService,
                                   @Qualifier("testExecutionExecutor") ThreadPoolTaskExecutor taskExecutor) {
        this.logStreamingService = logStreamingService;
        this.allureConfigurationService = allureConfigurationService;
        this.reportService = reportService;
        this.taskExecutor = taskExecutor;
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
        
        // Start execution in background task executor
        taskExecutor.execute(() -> {
            try {
                MDC.put("executionId", executionId);
                runTests(executionId, selectedTests, context);
            } finally {
                MDC.remove("executionId");
            }
        });
        
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
        AllureLifecycle allureLifecycle = null;
        try {
            // Send initial log message
            logStreamingService.publishLog(executionId, "=== Test Execution Started ===\n");
            logStreamingService.publishLog(executionId, String.format("Execution ID: %s%n", executionId));
            logStreamingService.publishLog(executionId, String.format("Selected tests: %d%n", selectedTests.size()));
            logStreamingService.publishLog(executionId, "==============================\n");
            
            // Configure Allure for this execution
            allureLifecycle = allureConfigurationService.createLifecycleForExecution(executionId);
            
            // Build discovery request with selected test IDs
            LauncherDiscoveryRequestBuilder requestBuilder = LauncherDiscoveryRequestBuilder.request();
            for (String testId : selectedTests) {
                requestBuilder.selectors(selectUniqueId(testId));
            }
            LauncherDiscoveryRequest request = requestBuilder.build();
            
            // Create launcher and register listeners
            Launcher launcher = LauncherFactory.create();
            CustomTestExecutionListener listener = new CustomTestExecutionListener(executionId, context, logStreamingService, allureLifecycle);
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
        
        public String getReportId() {
            return reportId;
        }
        
        public void setReportId(String reportId) {
            this.reportId = reportId;
        }
    }
    
    /**
     * Custom listener to capture test execution events and record to Allure.
     */
    private static class CustomTestExecutionListener implements TestExecutionListener {
        private final String executionId;
        private final ExecutionContext context;
        private final LogStreamingService logStreamingService;
        private final AllureLifecycle allureLifecycle;
        
        public CustomTestExecutionListener(String executionId, ExecutionContext context, LogStreamingService logStreamingService, AllureLifecycle allureLifecycle) {
            this.executionId = executionId;
            this.context = context;
            this.logStreamingService = logStreamingService;
            this.allureLifecycle = allureLifecycle;
        }
        
        @Override
        public void executionStarted(TestIdentifier testIdentifier) {
            // Check for cancellation
            if (context.getStatus() == ExecutionStatus.CANCELLED) {
                return;
            }

            if (testIdentifier.isTest()) {
                String message = String.format("[TEST STARTED] %s%n", testIdentifier.getDisplayName());
                // Publish directly to ensure it's sent
                logStreamingService.publishLog(executionId, message);
                logger.debug("Test started: {} [{}]}", testIdentifier.getDisplayName(), executionId);

                // Record test start to Allure
                String testCaseId = testIdentifier.getUniqueId();
                allureLifecycle.startTestCase(testCaseId);
                allureLifecycle.updateTestCase(testCaseId, result -> {
                    result.setName(testIdentifier.getDisplayName());
                    result.setFullName(testIdentifier.getDisplayName());
                    // Add labels
                    result.getLabels().add(new Label().setName("suite").setValue(testIdentifier.getSource().map(Object::toString).orElse("Unknown")));
                    result.getLabels().add(new Label().setName("testId").setValue(testCaseId));
                });
            }
        }
        
        @Override
        public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
            if (testIdentifier.isTest()) {
                String status = testExecutionResult.getStatus().name();
                String message = String.format("[TEST FINISHED] %s - Status: %s%n",
                    testIdentifier.getDisplayName(), status);
                // Publish directly to ensure it's sent
                logStreamingService.publishLog(executionId, message);
                logger.debug("Test finished: {} - Status: {} [{}]",
                    testIdentifier.getDisplayName(), status, executionId);

                // Record test finish to Allure
                String testCaseId = testIdentifier.getUniqueId();
                allureLifecycle.updateTestCase(testCaseId, testResult -> {
                    testResult.setStatus(convertToAllureStatus(testExecutionResult.getStatus()));

                    // Handle failure details
                    if (testExecutionResult.getStatus() == org.junit.platform.engine.TestExecutionResult.Status.FAILED) {
                        testExecutionResult.getThrowable().ifPresent(throwable -> {
                            testResult.setStatusDetails(new io.qameta.allure.model.StatusDetails()
                                .setMessage(throwable.getMessage())
                                .setTrace(getStackTraceAsString(throwable)));
                        });
                    }
                });

                allureLifecycle.stopTestCase(testCaseId);
                allureLifecycle.writeTestCase(testCaseId);
            }
        }
        
        @Override
        public void executionSkipped(TestIdentifier testIdentifier, String reason) {
            if (testIdentifier.isTest()) {
                String message = String.format("[TEST SKIPPED] %s - Reason: %s%n",
                    testIdentifier.getDisplayName(), reason);
                // Publish directly to ensure it's sent
                logStreamingService.publishLog(executionId, message);
                logger.debug("Test skipped: {} - Reason: {} [{}]",
                    testIdentifier.getDisplayName(), reason, executionId);

                // Record test skipped to Allure
                String testCaseId = testIdentifier.getUniqueId();
                allureLifecycle.startTestCase(testCaseId);
                allureLifecycle.updateTestCase(testCaseId, result -> {
                    result.setName(testIdentifier.getDisplayName());
                    result.setFullName(testIdentifier.getDisplayName());
                    result.setStatus(Status.SKIPPED);
                    result.setStatusDetails(new io.qameta.allure.model.StatusDetails().setMessage(reason));
                });
                allureLifecycle.stopTestCase(testCaseId);
                allureLifecycle.writeTestCase(testCaseId);
            }
        }
        
        private Status convertToAllureStatus(org.junit.platform.engine.TestExecutionResult.Status status) {
            switch (status) {
                case SUCCESSFUL:
                    return Status.PASSED;
                case FAILED:
                    return Status.FAILED;
                case ABORTED:
                    return Status.BROKEN;
                default:
                    return Status.SKIPPED;
            }
        }
        
        private String getStackTraceAsString(Throwable throwable) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            throwable.printStackTrace(pw);
            return sw.toString();
        }
    }
}
