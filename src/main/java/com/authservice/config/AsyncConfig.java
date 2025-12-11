package com.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async Configuration for Background Tasks
 * 
 * This configuration enables async processing for:
 * - Email sending (verification, welcome, password reset)
 * - SMS sending
 * - Any other @Async annotated methods
 * 
 * Benefits:
 * - API responses return immediately (no waiting for emails)
 * - Emails sent in background threads
 * - Better user experience with fast response times
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Configure thread pool for async email operations
     * 
     * Settings explained:
     * - corePoolSize: 2 threads always ready
     * - maxPoolSize: can grow up to 5 threads under load
     * - queueCapacity: can queue 100 tasks before rejecting
     * - threadNamePrefix: helps identify async threads in logs
     * 
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core pool size - threads always available
        executor.setCorePoolSize(2);
        
        // Maximum pool size - threads under heavy load
        executor.setMaxPoolSize(5);
        
        // Queue capacity - pending tasks before rejection
        executor.setQueueCapacity(100);
        
        // Thread naming for easier debugging
        executor.setThreadNamePrefix("async-email-");
        
        // Initialize the executor
        executor.initialize();
        
        return executor;
    }
}