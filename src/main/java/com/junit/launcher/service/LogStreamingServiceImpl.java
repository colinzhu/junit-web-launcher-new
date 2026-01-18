package com.junit.launcher.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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
        
        // Send initial connection event to confirm the connection is established
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data("Connected to log stream for execution: " + executionId));
            
            // Send any previously captured logs
            StringBuilder existingLogs = capturedLogs.get(executionId);
            if (existingLogs != null && existingLogs.length() > 0) {
                emitter.send(SseEmitter.event()
                    .name("log")
                    .data(existingLogs.toString()));
            }
        } catch (IOException e) {
            logger.debug("Failed to send initial event for execution: {}", executionId, e);
            removeEmitter(executionId, emitter);
        }
        
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
}
