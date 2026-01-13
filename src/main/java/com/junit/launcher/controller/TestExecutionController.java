package com.junit.launcher.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.junit.launcher.model.ExecutionRequest;
import com.junit.launcher.model.ExecutionResponse;
import com.junit.launcher.model.ExecutionStatus;
import com.junit.launcher.service.LogStreamingService;
import com.junit.launcher.service.TestExecutionService;

/**
 * REST controller for test execution operations.
 */
@RestController
@RequestMapping("/api")
public class TestExecutionController {
    
    private final TestExecutionService testExecutionService;
    private final LogStreamingService logStreamingService;
    
    public TestExecutionController(TestExecutionService testExecutionService,
                                   LogStreamingService logStreamingService) {
        this.testExecutionService = testExecutionService;
        this.logStreamingService = logStreamingService;
    }
    
    /**
     * Executes selected test cases.
     * 
     * @param request ExecutionRequest with selected test IDs
     * @return ExecutionResponse with execution ID and status
     */
    @PostMapping("/execute")
    public ResponseEntity<ExecutionResponse> executeTests(@RequestBody ExecutionRequest request) {
        try {
            if (request.getSelectedTestIds() == null || request.getSelectedTestIds().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            String executionId = testExecutionService.executeTests(request.getSelectedTestIds());
            
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            ExecutionResponse response = new ExecutionResponse(
                    executionId,
                    ExecutionStatus.RUNNING.name(),
                    timestamp
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Streams logs for a test execution via Server-Sent Events.
     * 
     * @param executionId The execution ID to stream logs for
     * @return SseEmitter for streaming log messages
     */
    @GetMapping("/stream/{executionId}")
    public ResponseEntity<SseEmitter> streamLogs(@PathVariable String executionId) {
        try {
            SseEmitter emitter = logStreamingService.streamLogs(executionId);
            return ResponseEntity.ok(emitter);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Gets the current status of a test execution.
     * 
     * @param executionId The execution ID to check
     * @return ExecutionStatus with current status
     */
    @GetMapping("/status/{executionId}")
    public ResponseEntity<ExecutionResponse> getExecutionStatus(@PathVariable String executionId) {
        try {
            ExecutionStatus status = testExecutionService.getExecutionStatus(executionId);
            
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            ExecutionResponse response = new ExecutionResponse(
                    executionId,
                    status.name(),
                    timestamp
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    
    /**
     * Cancels a running test execution.
     * 
     * @param executionId The execution ID to cancel
     * @return Response indicating success or failure
     */
    @PostMapping("/cancel/{executionId}")
    public ResponseEntity<Void> cancelExecution(@PathVariable String executionId) {
        try {
            testExecutionService.cancelExecution(executionId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
