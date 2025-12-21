package com.authservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

/**
 * TEMPORARY Migration Controller
 *
 * This controller is TEMPORARY and should be DELETED after running the migration.
 *
 * Purpose: Drop the old unique constraint on the 'email' column
 * and allow the composite unique constraint (email + app_source) to work.
 *
 * TO DELETE THIS FILE AFTER USE!
 */
@RestController
@RequestMapping("/api/migration")
@Slf4j
public class TempMigrationController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Drop the old unique constraint on email column
     *
     * Call this endpoint ONCE, then DELETE this controller file!
     *
     * URL: POST https://auth-microservice-stuf.onrender.com/api/migration/drop-old-email-constraint
     */
    @PostMapping("/drop-old-email-constraint")
    public ResponseEntity<String> dropOldEmailConstraint() {
        try {
            log.warn("🔧 MIGRATION: Attempting to drop old unique constraint on email column");

            // Drop the old unique constraint
            String sql = "ALTER TABLE users DROP CONSTRAINT IF EXISTS uk6dotkott2kjsp8vw4d0m25fb7";
            jdbcTemplate.execute(sql);

            log.info("✅ MIGRATION: Successfully dropped old email constraint");
            log.warn("⚠️ REMEMBER TO DELETE TempMigrationController.java AFTER THIS!");

            return ResponseEntity.ok(
                "✅ Migration successful! Old unique constraint dropped.\n\n" +
                "The composite unique constraint (email + app_source) is now active.\n\n" +
                "⚠️ IMPORTANT: DELETE src/main/java/com/authservice/controller/TempMigrationController.java\n" +
                "and redeploy your application!"
            );

        } catch (Exception e) {
            log.error("❌ MIGRATION FAILED: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body("❌ Migration failed: " + e.getMessage());
        }
    }

    /**
     * Check if the old constraint still exists
     */
    @PostMapping("/check-constraints")
    public ResponseEntity<String> checkConstraints() {
        try {
            log.info("🔍 Checking constraints on users table");

            String sql = "SELECT constraint_name FROM information_schema.table_constraints " +
                        "WHERE table_name = 'users' AND constraint_type = 'UNIQUE'";

            var constraints = jdbcTemplate.queryForList(sql, String.class);

            StringBuilder response = new StringBuilder("Current unique constraints on users table:\n\n");
            for (String constraint : constraints) {
                response.append("- ").append(constraint).append("\n");
            }

            if (constraints.contains("uk6dotkott2kjsp8vw4d0m25fb7")) {
                response.append("\n⚠️ Old constraint (uk6dotkott2kjsp8vw4d0m25fb7) still exists!\n");
                response.append("Run /drop-old-email-constraint to remove it.");
            } else {
                response.append("\n✅ Old constraint has been removed!");
            }

            return ResponseEntity.ok(response.toString());

        } catch (Exception e) {
            log.error("❌ Check failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body("❌ Check failed: " + e.getMessage());
        }
    }
}
