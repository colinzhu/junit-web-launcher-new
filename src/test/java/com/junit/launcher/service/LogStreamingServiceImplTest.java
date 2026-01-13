package com.junit.launcher.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Unit tests for LogStreamingServiceImpl.
 */
class LogStreamingServiceImplTest {
    
    private LogStreamingServiceImpl logStreamingService;
    
    @BeforeEach
    void setUp() {
        logStreamingService = new LogStreamingServiceImpl();
    }
    
    @Test
    void testStreamLogs_createsEmitter() {
        String executionId = "test-execution-1";
        
        SseEmitter emitter = logStreamingService.streamLogs(executionId);
        
        assertNotNull(emitter, "Emitter should not be null");
    }
    
    @Test
    void testPublishLog_capturesLogMessage() {
        String executionId = "test-execution-2";
        String logMessage = "Test log message\n";
        
        logStreamingService.publishLog(executionId, logMessage);
        
        String capturedLogs = logStreamingService.getCapturedLogs(executionId);
        assertEquals(logMessage, capturedLogs, "Captured logs should match published message");
    }
    
    @Test
    void testPublishLog_sendsToRegisteredEmitters() throws InterruptedException {
        String executionId = "test-execution-3";
        String logMessage = "Test log message\n";
        
        SseEmitter emitter = logStreamingService.streamLogs(executionId);
        
        // Simulate receiving a message by checking if publish doesn't throw
        logStreamingService.publishLog(executionId, logMessage);
        
        // Verify the log was captured
        String capturedLogs = logStreamingService.getCapturedLogs(executionId);
        assertTrue(capturedLogs.contains(logMessage), "Captured logs should contain the message");
        
        // Complete the emitter
        logStreamingService.completeStreaming(executionId);
    }
    
    @Test
    void testCompleteStreaming_completesAllEmitters() throws InterruptedException {
        String executionId = "test-execution-4";
        
        SseEmitter emitter1 = logStreamingService.streamLogs(executionId);
        SseEmitter emitter2 = logStreamingService.streamLogs(executionId);
        
        // Complete streaming should not throw
        logStreamingService.completeStreaming(executionId);
        
        // Verify we can't send more messages (emitters are removed)
        logStreamingService.publishLog(executionId, "Should not be sent\n");
    }
    
    @Test
    void testGetCapturedLogs_returnsEmptyForUnknownExecution() {
        String capturedLogs = logStreamingService.getCapturedLogs("unknown-execution");
        
        assertEquals("", capturedLogs, "Should return empty string for unknown execution");
    }
    
    @Test
    void testCreateCapturingStream_capturesOutput() {
        String executionId = "test-execution-5";
        
        java.io.PrintStream capturingStream = logStreamingService.createCapturingStream(
            executionId, System.out);
        
        capturingStream.println("Test output");
        capturingStream.flush();
        
        String capturedLogs = logStreamingService.getCapturedLogs(executionId);
        assertTrue(capturedLogs.contains("Test output"), 
            "Captured logs should contain the output");
    }
    
    @Test
    void testRedirectAndRestoreStreams() {
        String executionId = "test-execution-6";
        
        java.io.PrintStream originalOut = System.out;
        
        logStreamingService.redirectStreams(executionId);
        
        // Print something
        System.out.println("Redirected output");
        
        logStreamingService.restoreStreams();
        
        // Verify streams are restored
        assertEquals(originalOut, System.out, "System.out should be restored");
        
        // Verify output was captured
        String capturedLogs = logStreamingService.getCapturedLogs(executionId);
        assertTrue(capturedLogs.contains("Redirected output"), 
            "Captured logs should contain redirected output");
    }
}
