package com.authservice.model;

/**
 * AppSource enum - Tracks which application a user registered from
 * Allows one auth microservice to serve multiple applications
 */
public enum AppSource {
    RANDOM_WRITES,    // randomwritesrandomlights.com
    SECPLUS_PREP      // secplus-prep.com
}
