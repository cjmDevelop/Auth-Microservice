package com.authservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller
 * 
 * Purpose: Reduce Render's inactivity time
 * 
 * How it works:
 * 1. External cron service pings this endpoint every 10 minutes
 * 2. Keeps server awake 24/7
 * 3. Eliminates cold starts
 * 
 * Free cron services:
 * - cron-job.org 
 * - UptimeRobot
 * - Cronitor
 */
@RestController
public class HealthCheckController {

    /**
     * Simple health check endpoint
     * Returns server status and timestamp
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Auth Microservice");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        response.put("message", "Server is awake and ready! 🚀");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Ping endpoint (even simpler)
     * Returns just "pong"
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}