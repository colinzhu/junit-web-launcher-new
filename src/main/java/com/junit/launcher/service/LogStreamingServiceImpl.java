package com.junit.launcher.service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Implementation of LogStreamingService for streaming test execution logs via SSE.
 */
@Service
public class LogStreamingServiceImpl implements LogStreamingService {
    
    private static final Logger logger = LoggerFactory.getLogger(LogStreamingServiceImpl.class);
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutes
    
    // Map of execution IDs to list of SSE emitters
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    
    // Map of execution IDs to captured log content
    private final Map<String, StringBuilder> capturedLogs = new ConcurrentHashMap<>();
    
    // Store original streams
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    @Override
    public SseEmitter streamLogs(String executionId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        
        // Add emitter to the list for this execution
        emitters.computeIfAbsent(executionId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        
        logger.debug("SSE client registered for execution: {}", executionId);
        
        // Handle emitter completion
        emitter.onCompletion(() -> {
            removeEmitter(executionId, emitter);
            logger.debug("SSE client completed for execution: {}", executionId);
        });
        
        // Handle emitter timeout
        emitter.onTimeout(() -> {
            removeEmitter(executionId, emitter);
            logger.debug("SSE client timed out for execution: {}", executionId);
        });
        
        // Handle emitter error
        emitter.onError((ex) -> {
            removeEmitter(executionId, emitter);
            logger.debug("SSE client error for execution: {}", executionId, ex);
        });
        
        return emitter;
    }
    
    @Override
    public void publishLog(String executionId, String message) {
        // Capture the log message
        capturedLogs.computeIfAbsent(executionId, k -> new StringBuilder()).append(message);
        
        // Send to all registered emitters for this execution
        List<SseEmitter> executionEmitters = emitters.get(executionId);
        if (executionEmitters != null) {
            for (SseEmitter emitter : executionEmitters) {
                try {
                    emitter.send(SseEmitter.event()
                        .name("log")
                        .data(message));
                } catch (IOException e) {
                    logger.debug("Failed to send log to SSE client for execution: {}", executionId, e);
                    removeEmitter(executionId, emitter);
                }
            }
        }
    }
    
    @Override
    public void completeStreaming(String executionId) {
        List<SseEmitter> executionEmitters = emitters.get(executionId);
        if (executionEmitters != null) {
            for (SseEmitter emitter : executionEmitters) {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    logger.debug("Error completing SSE emitter for execution: {}", executionId, e);
                }
            }
            emitters.remove(executionId);
        }
        logger.debug("Streaming completed for execution: {}", executionId);
    }
    
    @Override
    public String getCapturedLogs(String executionId) {
        StringBuilder logs = capturedLogs.get(executionId);
        return logs != null ? logs.toString() : "";
    }
    
    /**
     * Removes an emitter from the list.
     */
    private void removeEmitter(String executionId, SseEmitter emitter) {
        List<SseEmitter> executionEmitters = emitters.get(executionId);
        if (executionEmitters != null) {
            executionEmitters.remove(emitter);
            if (executionEmitters.isEmpty()) {
                emitters.remove(executionId);
            }
        }
    }
    
    /**
     * Creates a custom PrintStream that captures output and forwards to log streaming.
     */
    public PrintStream createCapturingStream(String executionId, PrintStream original) {
        return new PrintStream(new OutputStream() {
            private final StringBuilder lineBuffer = new StringBuilder();
            
            @Override
            public void write(int b) throws IOException {
                // Write to original stream
                original.write(b);
                
                // Capture for streaming
                char c = (char) b;
                lineBuffer.append(c);
                
                // If newline, publish the line
                if (c == '\n') {
                    String line = lineBuffer.toString();
                    publishLog(executionId, line);
                    lineBuffer.setLength(0);
                }
            }
            
            @Override
            public void flush() throws IOException {
                original.flush();
                
                // Flush any remaining content
                if (lineBuffer.length() > 0) {
                    String line = lineBuffer.toString();
                    publishLog(executionId, line);
                    lineBuffer.setLength(0);
                }
            }
        }, true);
    }
    
    /**
     * Redirects System.out and System.err to capture output.
     */
    public void redirectStreams(String executionId) {
        PrintStream capturingOut = createCapturingStream(executionId, originalOut);
        PrintStream capturingErr = createCapturingStream(executionId, originalErr);
        
        System.setOut(capturingOut);
        System.setErr(capturingErr);
        
        logger.debug("Streams redirected for execution: {}", executionId);
    }
    
    /**
     * Restores original System.out and System.err.
     */
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        logger.debug("Streams restored");
    }
}
