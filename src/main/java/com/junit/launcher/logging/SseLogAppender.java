package com.junit.launcher.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.junit.launcher.service.LogStreamingService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Custom Appender to stream logs via LogStreamingService.
 */
@Component
public class SseLogAppender extends AppenderBase<ILoggingEvent> implements ApplicationContextAware {

    private static LogStreamingService logStreamingService;
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) {
        applicationContext = context;
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (logStreamingService == null && applicationContext != null) {
            logStreamingService = applicationContext.getBean(LogStreamingService.class);
        }

        if (logStreamingService != null) {
            String executionId = event.getMDCPropertyMap().get("executionId");
            if (executionId != null) {
                logStreamingService.publishLog(executionId, event.getFormattedMessage() + "\n");
            }
        }
    }
}