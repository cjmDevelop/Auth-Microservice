package com.authservice.model;

/**
 * User roles in the form of enum for simplicity, structure and security.
 * Admin and Moderator are added incase of future development after regular user is implemented.
 */

public enum Role {
    USER,
    ADMIN,
    MODERATOR
}
