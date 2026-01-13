package com.junit.launcher.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Service for streaming test execution logs to clients via Server-Sent Events.
 */
public interface LogStreamingService {
    
    /**
     * Registers a client for log streaming.
     * 
     * @param executionId The execution to stream logs for
     * @return SseEmitter for streaming
     */
    SseEmitter streamLogs(String executionId);
    
    /**
     * Publishes a log message to all registered clients.
     * 
     * @param executionId The execution this log belongs to
     * @param message The log message
     */
    void publishLog(String executionId, String message);
    
    /**
     * Completes streaming for an execution.
     * 
     * @param executionId The execution to complete
     */
    void completeStreaming(String executionId);
    
    /**
     * Gets the captured logs for an execution.
     * 
     * @param executionId The execution ID
     * @return The captured log content
     */
    String getCapturedLogs(String executionId);
}
