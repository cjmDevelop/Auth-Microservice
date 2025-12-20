package com.authservice.service;

import com.authservice.model.AppSource;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Resend Email Service - OPTIMIZED VERSION
 * 
 * All email methods use @Async to send emails in background threads
 * This ensures API responses are instant (< 200ms)
 * 
 * Performance:
 * - Before: 1-2 minutes (blocking)
 * - After: < 200ms (async)
 */
@Service
@Slf4j
public class ResendEmailService {

    private final Resend resendClient;
    private final String fromEmail;
    private final JavaMailSender mailSender;

    @Value("${app.dev-mode:false}")
    private boolean devMode;

    @Value("${app.use-gmail:false}")
    private boolean useGmail;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    public ResendEmailService(
            @Value("${resend.api-key}") String apiKey,
            @Value("${resend.from-email}") String fromEmail,
            @Autowired(required = false) JavaMailSender mailSender
    ) {
        this.resendClient = new Resend(apiKey);
        this.fromEmail = fromEmail;
        this.mailSender = mailSender;
        log.info("✅ Email Service initialized - From: {}", fromEmail);
    }

    /**
     * Get the correct "from" email address based on the app source
     */
    private String getFromEmailForSource(AppSource appSource) {
        return appSource == AppSource.SECPLUS_PREP
            ? "noreply@secplus-prep.com"
            : fromEmail;
    }

    /**
     * Send verification email with code to user
     *
     * @Async - Runs in background thread, doesn't block API response
     */
    @Async("taskExecutor")
    public void sendVerificationEmail(String to, String code, String name, AppSource appSource) {
        long startTime = System.currentTimeMillis();

        // DEV MODE: Skip sending email
        if (devMode) {
            log.warn("🔧 DEV MODE: Skipping verification email to: {} (Code: {})", to, code);
            return;
        }

        String htmlContent = buildVerificationEmail(name != null ? name : "there", code, appSource);
        String subject = appSource == AppSource.SECPLUS_PREP
            ? "Verify Your Email Address - SecPlus Prep"
            : "Verify Your Email Address - Random Writes Random Lights";

        // Route email based on app source
        // SecPlus Prep uses Amazon SES SMTP
        // Random Writes uses Resend API
        if (appSource == AppSource.SECPLUS_PREP && mailSender != null) {
            sendViaSMTP(to, subject, htmlContent, startTime, "noreply@secplus-prep.com");
            return;
        } else if (useGmail && mailSender != null) {
            // Legacy Gmail support for testing
            sendViaSMTP(to, subject, htmlContent, startTime, smtpUsername);
            return;
        }

        // Otherwise use Resend API (for Random Writes)
        try {
            log.info("📧 Sending verification email to: {} via Resend", to);

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(getFromEmailForSource(appSource))
                    .to(to)
                    .subject(subject)
                    .html(htmlContent)
                    .build();

            CreateEmailResponse response = resendClient.emails().send(params);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Verification email sent to: {} with ID: {} (took {}ms)",
                     to, response.getId(), duration);

        } catch (ResendException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ Failed to send verification email to: {} (took {}ms)", to, duration, e);
            throw new RuntimeException("Failed to send verification email: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to send email via SMTP (Amazon SES or Gmail)
     */
    private void sendViaSMTP(String to, String subject, String htmlContent, long startTime, String fromEmail) {
        try {
            String smtpProvider = fromEmail.contains("secplus-prep") ? "Amazon SES" : "SMTP";
            log.info("📧 Sending email to: {} via {}", to, smtpProvider);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Email sent to: {} via {} (took {}ms)", to, smtpProvider, duration);

        } catch (MessagingException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ Failed to send email to: {} via SMTP (took {}ms)", to, duration, e);
            throw new RuntimeException("Failed to send email via SMTP: " + e.getMessage(), e);
        }
    }

    /**
     * Send welcome email after successful verification
     *
     * @Async - Runs in background thread
     */
    @Async("taskExecutor")
    public void sendWelcomeEmail(String to, String name, AppSource appSource) {
        long startTime = System.currentTimeMillis();

        // DEV MODE: Skip sending email
        if (devMode) {
            log.warn("🔧 DEV MODE: Skipping welcome email to: {}", to);
            return;
        }

        try {
            log.info("📧 Sending welcome email to: {}", to);

            String htmlContent = buildWelcomeEmail(name != null ? name : "there", appSource);
            String subject = appSource == AppSource.SECPLUS_PREP
                ? "Welcome to SecPlus Prep! 🎉"
                : "Welcome to Random Writes Random Lights! 🎉";

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(getFromEmailForSource(appSource))
                    .to(to)
                    .subject(subject)
                    .html(htmlContent)
                    .build();

            CreateEmailResponse response = resendClient.emails().send(params);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Welcome email sent to: {} with ID: {} (took {}ms)",
                     to, response.getId(), duration);

        } catch (ResendException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ Failed to send welcome email to: {} (took {}ms)", to, duration, e);
            // Don't throw - welcome email failure shouldn't break registration
        }
    }

    /**
     * Send password reset email with 6-digit code
     *
     * @Async - Runs in background thread, API returns immediately
     */
    @Async("taskExecutor")
    public void sendPasswordResetEmail(String to, String username, String resetCode, AppSource appSource) {
        long startTime = System.currentTimeMillis();

        // DEV MODE: Skip sending email but log the code
        if (devMode) {
            log.warn("🔧 DEV MODE: Skipping password reset email to: {} (Reset Code: {})", to, resetCode);
            return;
        }

        String htmlContent = buildPasswordResetEmail(username != null ? username : "there", resetCode, appSource);
        String subject = appSource == AppSource.SECPLUS_PREP
            ? "Reset Your Password - SecPlus Prep 🔐"
            : "Reset Your Password - Random Writes Random Lights 🔐";

        // Route email based on app source
        // SecPlus Prep uses Amazon SES SMTP
        // Random Writes uses Resend API
        if (appSource == AppSource.SECPLUS_PREP && mailSender != null) {
            sendViaSMTP(to, subject, htmlContent, startTime, "noreply@secplus-prep.com");
            return;
        } else if (useGmail && mailSender != null) {
            // Legacy Gmail support for testing
            sendViaSMTP(to, subject, htmlContent, startTime, smtpUsername);
            return;
        }

        // Otherwise use Resend API (for Random Writes)
        try {
            log.info("📧 Sending password reset email to: {} via Resend", to);

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(getFromEmailForSource(appSource))
                    .to(to)
                    .subject(subject)
                    .html(htmlContent)
                    .build();

            CreateEmailResponse response = resendClient.emails().send(params);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Password reset email sent to: {} with ID: {} (took {}ms)",
                     to, response.getId(), duration);

        } catch (ResendException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ Failed to send password reset email to: {} (took {}ms)", to, duration, e);
            throw new RuntimeException("Failed to send password reset email: " + e.getMessage(), e);
        }
    }

    /**
     * Send confirmation email after password change
     *
     * @Async - Runs in background thread
     */
    @Async("taskExecutor")
    public void sendPasswordChangedConfirmation(String to, String username, AppSource appSource) {
        long startTime = System.currentTimeMillis();

        // DEV MODE: Skip sending email
        if (devMode) {
            log.warn("🔧 DEV MODE: Skipping password changed confirmation to: {}", to);
            return;
        }

        try {
            log.info("📧 Sending password changed confirmation to: {}", to);

            String htmlContent = buildPasswordChangedEmail(username != null ? username : "there", appSource);

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(getFromEmailForSource(appSource))
                    .to(to)
                    .subject("Password Changed Successfully ✅")
                    .html(htmlContent)
                    .build();

            CreateEmailResponse response = resendClient.emails().send(params);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Password changed confirmation sent to: {} with ID: {} (took {}ms)",
                     to, response.getId(), duration);

        } catch (ResendException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ Failed to send password changed confirmation to: {} (took {}ms)", to, duration, e);
            // Don't throw - confirmation email failure shouldn't break password reset
        }
    }

    // ==================== EMAIL TEMPLATES ====================

    private String buildVerificationEmail(String name, String code, AppSource appSource) {
        if (appSource == AppSource.SECPLUS_PREP) {
            return buildVerificationEmailSecPlusPrep(name, code);
        }
        return buildVerificationEmailRandomWrites(name, code);
    }

    private String buildVerificationEmailRandomWrites(String name, String code) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { 
                            font-family: Arial, sans-serif; 
                            line-height: 1.6; 
                            background-color: #000;
                            margin: 0;
                            padding: 20px;
                        }
                        .container { 
                            max-width: 600px; 
                            margin: 20px auto; 
                            background: #111;
                            border-radius: 10px;
                            overflow: hidden;
                            box-shadow: 0 0 30px rgba(255, 215, 0, 0.3);
                            border: 2px solid #333;
                        }
                        .header { 
                            background: linear-gradient(135deg, #1a1a1a 0%%, #2a2a2a 100%%);
                            color: yellow;
                            padding: 30px 20px; 
                            text-align: center;
                            border-bottom: 2px solid yellow;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 28px;
                            text-shadow: 0 0 10px rgba(255, 255, 0, 0.5);
                        }
                        .content { 
                            padding: 40px 30px;
                            color: #ddd;
                        }
                        .code-box { 
                            font-size: 36px; 
                            font-weight: bold; 
                            color: yellow;
                            text-align: center; 
                            padding: 25px; 
                            background: #222;
                            border: 2px dashed yellow;
                            border-radius: 8px; 
                            margin: 30px 0; 
                            letter-spacing: 8px;
                            text-shadow: 0 0 15px rgba(255, 255, 0, 0.8);
                        }
                        .footer { 
                            text-align: center; 
                            padding: 20px; 
                            background: #0a0a0a;
                            color: #666;
                            font-size: 12px; 
                        }
                        .warning {
                            background: rgba(255, 193, 7, 0.1);
                            border-left: 4px solid #ffc107;
                            padding: 15px;
                            margin: 20px 0;
                            color: #ffc107;
                        }
                        strong {
                            color: yellow;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>✉️ Email Verification</h1>
                        </div>
                        <div class="content">
                            <p>Hi <strong>%s</strong>,</p>
                            <p>Thank you for joining <strong>Random Writes Random Lights</strong>! 🎉</p>
                            <p>Please use the verification code below to verify your email address:</p>
                            <div class="code-box">%s</div>
                            <p>This code will expire in <strong>15 minutes</strong>.</p>
                            <div class="warning">
                                ⚠️ If you didn't create an account, please ignore this email.
                            </div>
                        </div>
                        <div class="footer">
                            <p>This is an automated message, please do not reply.</p>
                            <p>&copy; 2025 Random Writes Random Lights. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(name, code);
    }

    private String buildVerificationEmailSecPlusPrep(String name, String code) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            line-height: 1.6;
                            background-color: #0a0e27;
                            margin: 0;
                            padding: 20px;
                        }
                        .container {
                            max-width: 600px;
                            margin: 20px auto;
                            background: linear-gradient(135deg, #1a1f3a 0%%, #0f1729 100%%);
                            border-radius: 10px;
                            overflow: hidden;
                            box-shadow: 0 0 40px rgba(0, 123, 255, 0.4);
                            border: 2px solid #007bff;
                        }
                        .header {
                            background: linear-gradient(135deg, #007bff 0%%, #0056b3 100%%);
                            color: white;
                            padding: 30px 20px;
                            text-align: center;
                            border-bottom: 3px solid #0056b3;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 28px;
                            text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
                        }
                        .content {
                            padding: 40px 30px;
                            color: #e8e8e8;
                        }
                        .code-box {
                            font-size: 36px;
                            font-weight: bold;
                            color: #007bff;
                            text-align: center;
                            padding: 25px;
                            background: rgba(0, 123, 255, 0.1);
                            border: 2px dashed #007bff;
                            border-radius: 8px;
                            margin: 30px 0;
                            letter-spacing: 8px;
                            text-shadow: 0 0 15px rgba(0, 123, 255, 0.6);
                        }
                        .footer {
                            text-align: center;
                            padding: 20px;
                            background: #060a18;
                            color: #666;
                            font-size: 12px;
                        }
                        .warning {
                            background: rgba(255, 193, 7, 0.1);
                            border-left: 4px solid #ffc107;
                            padding: 15px;
                            margin: 20px 0;
                            color: #ffc107;
                        }
                        strong {
                            color: #007bff;
                        }
                        .badge {
                            display: inline-block;
                            background: #28a745;
                            color: white;
                            padding: 5px 15px;
                            border-radius: 20px;
                            font-size: 14px;
                            font-weight: bold;
                            margin: 10px 0;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🔐 Email Verification</h1>
                            <div class="badge">Security+ SY0-701</div>
                        </div>
                        <div class="content">
                            <p>Hi <strong>%s</strong>,</p>
                            <p>Thank you for joining <strong>SecPlus Prep</strong>! 🎯</p>
                            <p>Your journey to Security+ certification starts here. Please use the verification code below to verify your email address:</p>
                            <div class="code-box">%s</div>
                            <p>This code will expire in <strong>15 minutes</strong>.</p>
                            <div class="warning">
                                ⚠️ If you didn't create an account, please ignore this email.
                            </div>
                        </div>
                        <div class="footer">
                            <p>This is an automated message, please do not reply.</p>
                            <p>&copy; 2025 SecPlus Prep. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(name, code);
    }

    private String buildWelcomeEmail(String name, AppSource appSource) {
        if (appSource == AppSource.SECPLUS_PREP) {
            return buildWelcomeEmailSecPlusPrep(name);
        }
        return buildWelcomeEmailRandomWrites(name);
    }

    private String buildWelcomeEmailRandomWrites(String name) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { 
                            font-family: Arial, sans-serif; 
                            line-height: 1.6; 
                            background-color: #000;
                            margin: 0;
                            padding: 20px;
                        }
                        .container { 
                            max-width: 600px; 
                            margin: 20px auto; 
                            background: #111;
                            border-radius: 10px;
                            overflow: hidden;
                            box-shadow: 0 0 30px rgba(57, 255, 20, 0.3);
                            border: 2px solid #333;
                        }
                        .header { 
                            background: linear-gradient(135deg, #1a1a1a 0%%, #2a2a2a 100%%);
                            color: #39ff14;
                            padding: 30px 20px; 
                            text-align: center;
                            border-bottom: 2px solid #39ff14;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 28px;
                            text-shadow: 0 0 10px rgba(57, 255, 20, 0.5);
                        }
                        .content { 
                            padding: 40px 30px;
                            color: #ddd;
                        }
                        .footer { 
                            text-align: center; 
                            padding: 20px; 
                            background: #0a0a0a;
                            color: #666;
                            font-size: 12px; 
                        }
                        strong {
                            color: #39ff14;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🎉 Welcome!</h1>
                        </div>
                        <div class="content">
                            <p>Hi <strong>%s</strong>,</p>
                            <p>Welcome to <strong>Random Writes Random Lights</strong>! 🧠⚡️</p>
                            <p>Your email has been successfully verified, and you're all set to start capturing your random thoughts and ideas!</p>
                            <p>Every brilliant idea starts with a single thought. Start writing yours today! ✨</p>
                            <p>If you have any questions, feel free to reach out to our support team.</p>
                        </div>
                        <div class="footer">
                            <p>Thank you for joining us!</p>
                            <p>&copy; 2025 Random Writes Random Lights. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(name);
    }

    private String buildWelcomeEmailSecPlusPrep(String name) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            line-height: 1.6;
                            background-color: #0a0e27;
                            margin: 0;
                            padding: 20px;
                        }
                        .container {
                            max-width: 600px;
                            margin: 20px auto;
                            background: linear-gradient(135deg, #1a1f3a 0%%, #0f1729 100%%);
                            border-radius: 10px;
                            overflow: hidden;
                            box-shadow: 0 0 40px rgba(40, 167, 69, 0.4);
                            border: 2px solid #28a745;
                        }
                        .header {
                            background: linear-gradient(135deg, #28a745 0%%, #1e7e34 100%%);
                            color: white;
                            padding: 30px 20px;
                            text-align: center;
                            border-bottom: 3px solid #1e7e34;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 28px;
                            text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
                        }
                        .content {
                            padding: 40px 30px;
                            color: #e8e8e8;
                        }
                        .footer {
                            text-align: center;
                            padding: 20px;
                            background: #060a18;
                            color: #666;
                            font-size: 12px;
                        }
                        strong {
                            color: #28a745;
                        }
                        .badge {
                            display: inline-block;
                            background: #007bff;
                            color: white;
                            padding: 5px 15px;
                            border-radius: 20px;
                            font-size: 14px;
                            font-weight: bold;
                            margin: 10px 0;
                        }
                        .success-icon {
                            font-size: 64px;
                            text-align: center;
                            margin: 20px 0;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🎉 Welcome!</h1>
                            <div class="badge">Security+ SY0-701</div>
                        </div>
                        <div class="content">
                            <div class="success-icon">🎯</div>
                            <p>Hi <strong>%s</strong>,</p>
                            <p>Welcome to <strong>SecPlus Prep</strong>! We're excited to help you on your Security+ certification journey! 🚀</p>
                            <p>Your email has been successfully verified, and you're all set to start preparing for the CompTIA Security+ SY0-701 exam!</p>
                            <p>Get ready to master security concepts, practice with exam-style questions, and earn your Security+ certification! 💪</p>
                            <p>If you have any questions, feel free to reach out to our support team.</p>
                        </div>
                        <div class="footer">
                            <p>Good luck on your certification journey!</p>
                            <p>&copy; 2025 SecPlus Prep. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(name);
    }

    private String buildPasswordResetEmail(String username, String resetCode, AppSource appSource) {
        if (appSource == AppSource.SECPLUS_PREP) {
            return buildPasswordResetEmailSecPlusPrep(username, resetCode);
        }
        return buildPasswordResetEmailRandomWrites(username, resetCode);
    }

    private String buildPasswordResetEmailRandomWrites(String username, String resetCode) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { 
                            font-family: Arial, sans-serif; 
                            line-height: 1.6; 
                            background-color: #000;
                            margin: 0;
                            padding: 20px;
                        }
                        .container { 
                            max-width: 600px; 
                            margin: 20px auto; 
                            background: #111;
                            border-radius: 10px;
                            overflow: hidden;
                            box-shadow: 0 0 30px rgba(255, 85, 85, 0.3);
                            border: 2px solid #333;
                        }
                        .header { 
                            background: linear-gradient(135deg, #1a1a1a 0%%, #2a2a2a 100%%);
                            color: #ff5555;
                            padding: 30px 20px; 
                            text-align: center;
                            border-bottom: 2px solid #ff5555;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 28px;
                            text-shadow: 0 0 10px rgba(255, 85, 85, 0.5);
                        }
                        .content { 
                            padding: 40px 30px;
                            color: #ddd;
                        }
                        .code-box { 
                            font-size: 36px; 
                            font-weight: bold; 
                            color: #ff5555;
                            text-align: center; 
                            padding: 25px; 
                            background: #222;
                            border: 2px dashed #ff5555;
                            border-radius: 8px; 
                            margin: 30px 0; 
                            letter-spacing: 8px;
                            text-shadow: 0 0 15px rgba(255, 85, 85, 0.8);
                        }
                        .warning {
                            background: rgba(255, 193, 7, 0.1);
                            border-left: 4px solid #ffc107;
                            padding: 15px;
                            margin: 20px 0;
                            color: #ffc107;
                        }
                        .footer { 
                            text-align: center; 
                            padding: 20px; 
                            background: #0a0a0a;
                            color: #666;
                            font-size: 12px; 
                        }
                        strong {
                            color: #ff5555;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🔐 Password Reset Request</h1>
                        </div>
                        <div class="content">
                            <p>Hi <strong>%s</strong>,</p>
                            <p>We received a request to reset your password for your <strong>Random Writes Random Lights</strong> account.</p>
                            <p>Use the code below to reset your password:</p>
                            <div class="code-box">%s</div>
                            <div class="warning">
                                <strong>⏰ This code will expire in 15 minutes</strong>
                            </div>
                            <p>If you didn't request a password reset, please ignore this email or contact support if you have concerns.</p>
                            <p>For security reasons, never share this code with anyone.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2025 Random Writes Random Lights. All rights reserved.</p>
                            <p>This is an automated email, please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(username, resetCode);
    }

    private String buildPasswordResetEmailSecPlusPrep(String username, String resetCode) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            line-height: 1.6;
                            background-color: #0a0e27;
                            margin: 0;
                            padding: 20px;
                        }
                        .container {
                            max-width: 600px;
                            margin: 20px auto;
                            background: linear-gradient(135deg, #1a1f3a 0%%, #0f1729 100%%);
                            border-radius: 10px;
                            overflow: hidden;
                            box-shadow: 0 0 40px rgba(220, 53, 69, 0.4);
                            border: 2px solid #dc3545;
                        }
                        .header {
                            background: linear-gradient(135deg, #dc3545 0%%, #c82333 100%%);
                            color: white;
                            padding: 30px 20px;
                            text-align: center;
                            border-bottom: 3px solid #c82333;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 28px;
                            text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
                        }
                        .content {
                            padding: 40px 30px;
                            color: #e8e8e8;
                        }
                        .code-box {
                            font-size: 36px;
                            font-weight: bold;
                            color: #dc3545;
                            text-align: center;
                            padding: 25px;
                            background: rgba(220, 53, 69, 0.1);
                            border: 2px dashed #dc3545;
                            border-radius: 8px;
                            margin: 30px 0;
                            letter-spacing: 8px;
                            text-shadow: 0 0 15px rgba(220, 53, 69, 0.6);
                        }
                        .warning {
                            background: rgba(255, 193, 7, 0.1);
                            border-left: 4px solid #ffc107;
                            padding: 15px;
                            margin: 20px 0;
                            color: #ffc107;
                        }
                        .footer {
                            text-align: center;
                            padding: 20px;
                            background: #060a18;
                            color: #666;
                            font-size: 12px;
                        }
                        strong {
                            color: #dc3545;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🔐 Password Reset Request</h1>
                        </div>
                        <div class="content">
                            <p>Hi <strong>%s</strong>,</p>
                            <p>We received a request to reset your password for your <strong>SecPlus Prep</strong> account.</p>
                            <p>Use the code below to reset your password:</p>
                            <div class="code-box">%s</div>
                            <div class="warning">
                                <strong>⏰ This code will expire in 15 minutes</strong>
                            </div>
                            <p>If you didn't request a password reset, please ignore this email or contact support if you have concerns.</p>
                            <p>For security reasons, never share this code with anyone.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2025 SecPlus Prep. All rights reserved.</p>
                            <p>This is an automated email, please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(username, resetCode);
    }

    private String buildPasswordChangedEmail(String username, AppSource appSource) {
        if (appSource == AppSource.SECPLUS_PREP) {
            return buildPasswordChangedEmailSecPlusPrep(username);
        }
        return buildPasswordChangedEmailRandomWrites(username);
    }

    private String buildPasswordChangedEmailRandomWrites(String username) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { 
                            font-family: Arial, sans-serif; 
                            line-height: 1.6; 
                            background-color: #000;
                            margin: 0;
                            padding: 20px;
                        }
                        .container { 
                            max-width: 600px; 
                            margin: 20px auto; 
                            background: #111;
                            border-radius: 10px;
                            overflow: hidden;
                            box-shadow: 0 0 30px rgba(57, 255, 20, 0.3);
                            border: 2px solid #333;
                        }
                        .header { 
                            background: linear-gradient(135deg, #1a1a1a 0%%, #2a2a2a 100%%);
                            color: #39ff14;
                            padding: 30px 20px; 
                            text-align: center;
                            border-bottom: 2px solid #39ff14;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 28px;
                            text-shadow: 0 0 10px rgba(57, 255, 20, 0.5);
                        }
                        .content { 
                            padding: 40px 30px;
                            color: #ddd;
                        }
                        .success-box { 
                            background: rgba(57, 255, 20, 0.1);
                            border: 1px solid #39ff14;
                            border-radius: 10px; 
                            padding: 20px; 
                            text-align: center; 
                            margin: 20px 0;
                        }
                        .success-box h2 {
                            color: #39ff14;
                            margin: 0;
                            text-shadow: 0 0 10px rgba(57, 255, 20, 0.5);
                        }
                        .warning {
                            background: rgba(255, 193, 7, 0.1);
                            border-left: 4px solid #ffc107;
                            padding: 15px;
                            margin: 20px 0;
                            color: #ffc107;
                        }
                        .footer { 
                            text-align: center; 
                            padding: 20px; 
                            background: #0a0a0a;
                            color: #666;
                            font-size: 12px; 
                        }
                        strong {
                            color: #39ff14;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>✅ Password Successfully Changed</h1>
                        </div>
                        <div class="content">
                            <p>Hi <strong>%s</strong>,</p>
                            <div class="success-box">
                                <h2>Your password has been changed successfully!</h2>
                            </div>
                            <p>Your <strong>Random Writes Random Lights</strong> account password was recently changed.</p>
                            <p>You can now log in with your new password.</p>
                            <div class="warning">
                                <strong>⚠️ Didn't change your password?</strong><br>
                                If you didn't make this change, please contact our support team immediately to secure your account.
                            </div>
                        </div>
                        <div class="footer">
                            <p>&copy; 2025 Random Writes Random Lights. All rights reserved.</p>
                            <p>This is an automated email, please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(username);
    }

    private String buildPasswordChangedEmailSecPlusPrep(String username) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            line-height: 1.6;
                            background-color: #0a0e27;
                            margin: 0;
                            padding: 20px;
                        }
                        .container {
                            max-width: 600px;
                            margin: 20px auto;
                            background: linear-gradient(135deg, #1a1f3a 0%%, #0f1729 100%%);
                            border-radius: 10px;
                            overflow: hidden;
                            box-shadow: 0 0 40px rgba(40, 167, 69, 0.4);
                            border: 2px solid #28a745;
                        }
                        .header {
                            background: linear-gradient(135deg, #28a745 0%%, #1e7e34 100%%);
                            color: white;
                            padding: 30px 20px;
                            text-align: center;
                            border-bottom: 3px solid #1e7e34;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 28px;
                            text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
                        }
                        .content {
                            padding: 40px 30px;
                            color: #e8e8e8;
                        }
                        .success-box {
                            background: rgba(40, 167, 69, 0.1);
                            border: 1px solid #28a745;
                            border-radius: 10px;
                            padding: 20px;
                            text-align: center;
                            margin: 20px 0;
                        }
                        .success-box h2 {
                            color: #28a745;
                            margin: 0;
                            text-shadow: 0 0 10px rgba(40, 167, 69, 0.5);
                        }
                        .warning {
                            background: rgba(255, 193, 7, 0.1);
                            border-left: 4px solid #ffc107;
                            padding: 15px;
                            margin: 20px 0;
                            color: #ffc107;
                        }
                        .footer {
                            text-align: center;
                            padding: 20px;
                            background: #060a18;
                            color: #666;
                            font-size: 12px;
                        }
                        strong {
                            color: #28a745;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>✅ Password Successfully Changed</h1>
                        </div>
                        <div class="content">
                            <p>Hi <strong>%s</strong>,</p>
                            <div class="success-box">
                                <h2>Your password has been changed successfully!</h2>
                            </div>
                            <p>Your <strong>SecPlus Prep</strong> account password was recently changed.</p>
                            <p>You can now log in with your new password and continue your Security+ certification journey!</p>
                            <div class="warning">
                                <strong>⚠️ Didn't change your password?</strong><br>
                                If you didn't make this change, please contact our support team immediately to secure your account.
                            </div>
                        </div>
                        <div class="footer">
                            <p>&copy; 2025 SecPlus Prep. All rights reserved.</p>
                            <p>This is an automated email, please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(username);
    }

    /**
     * Send account deletion confirmation email
     *
     * @Async - Runs in background thread
     */
    @Async("taskExecutor")
    public void sendAccountDeletionConfirmation(String to, String username, AppSource appSource) {
        long startTime = System.currentTimeMillis();

        // DEV MODE: Skip sending email
        if (devMode) {
            log.warn("🔧 DEV MODE: Skipping account deletion confirmation to: {}", to);
            return;
        }

        try {
            log.info("📧 Sending account deletion confirmation to: {}", to);

            String htmlContent = buildAccountDeletionEmail(username != null ? username : "there", appSource);

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(getFromEmailForSource(appSource))
                    .to(to)
                    .subject("Account Deleted - We're Sorry to See You Go 👋")
                    .html(htmlContent)
                    .build();

            CreateEmailResponse response = resendClient.emails().send(params);

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Account deletion confirmation sent to: {} with ID: {} (took {}ms)",
                     to, response.getId(), duration);

        } catch (ResendException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ Failed to send account deletion confirmation to: {} (took {}ms)", to, duration, e);
            // Don't throw - confirmation email failure shouldn't break account deletion
        }
    }

    /**
     * Email template for account deletion confirmation
     */
    private String buildAccountDeletionEmail(String username, AppSource appSource) {
        if (appSource == AppSource.SECPLUS_PREP) {
            return buildAccountDeletionEmailSecPlusPrep(username);
        }
        return buildAccountDeletionEmailRandomWrites(username);
    }

    private String buildAccountDeletionEmailRandomWrites(String username) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            line-height: 1.6;
                            background-color: #000;
                            margin: 0;
                            padding: 20px;
                        }
                        .container {
                            max-width: 600px;
                            margin: 20px auto;
                            background: #111;
                            border-radius: 10px;
                            overflow: hidden;
                            box-shadow: 0 0 30px rgba(255, 85, 85, 0.3);
                            border: 2px solid #333;
                        }
                        .header {
                            background: linear-gradient(135deg, #1a1a1a 0%%, #2a2a2a 100%%);
                            color: #ff5555;
                            padding: 30px 20px;
                            text-align: center;
                            border-bottom: 2px solid #ff5555;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 28px;
                            text-shadow: 0 0 10px rgba(255, 85, 85, 0.5);
                        }
                        .content {
                            padding: 40px 30px;
                            color: #ddd;
                        }
                        .info-box {
                            background: rgba(255, 193, 7, 0.1);
                            border: 1px solid #ffc107;
                            border-radius: 10px;
                            padding: 20px;
                            margin: 20px 0;
                            color: #ffc107;
                        }
                        .footer {
                            text-align: center;
                            padding: 20px;
                            background: #0a0a0a;
                            color: #666;
                            font-size: 12px;
                        }
                        strong {
                            color: #ff5555;
                        }
                        .goodbye {
                            text-align: center;
                            font-size: 48px;
                            margin: 20px 0;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>👋 Account Deleted</h1>
                        </div>
                        <div class="content">
                            <p>Hi <strong>%s</strong>,</p>
                            <div class="goodbye">💔</div>
                            <p>Your <strong>Random Writes Random Lights</strong> account has been permanently deleted as requested.</p>
                            <div class="info-box">
                                <strong>What's been deleted:</strong><br>
                                ✓ Your account information<br>
                                ✓ All your saved notes and ideas<br>
                                ✓ All verification tokens<br>
                                ✓ All password reset tokens
                            </div>
                            <p>We're sorry to see you go! If you change your mind, you're always welcome to create a new account.</p>
                            <p>Thank you for being part of our community. We hope Random Writes Random Lights helped capture your brilliant ideas! ✨</p>
                            <p><em>If you didn't request this deletion, please contact our support team immediately.</em></p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2025 Random Writes Random Lights. All rights reserved.</p>
                            <p>This is an automated email, please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(username);
    }

    private String buildAccountDeletionEmailSecPlusPrep(String username) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            line-height: 1.6;
                            background-color: #0a0e27;
                            margin: 0;
                            padding: 20px;
                        }
                        .container {
                            max-width: 600px;
                            margin: 20px auto;
                            background: linear-gradient(135deg, #1a1f3a 0%%, #0f1729 100%%);
                            border-radius: 10px;
                            overflow: hidden;
                            box-shadow: 0 0 40px rgba(220, 53, 69, 0.4);
                            border: 2px solid #dc3545;
                        }
                        .header {
                            background: linear-gradient(135deg, #dc3545 0%%, #c82333 100%%);
                            color: white;
                            padding: 30px 20px;
                            text-align: center;
                            border-bottom: 3px solid #c82333;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 28px;
                            text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
                        }
                        .content {
                            padding: 40px 30px;
                            color: #e8e8e8;
                        }
                        .info-box {
                            background: rgba(255, 193, 7, 0.1);
                            border: 1px solid #ffc107;
                            border-radius: 10px;
                            padding: 20px;
                            margin: 20px 0;
                            color: #ffc107;
                        }
                        .footer {
                            text-align: center;
                            padding: 20px;
                            background: #060a18;
                            color: #666;
                            font-size: 12px;
                        }
                        strong {
                            color: #dc3545;
                        }
                        .goodbye {
                            text-align: center;
                            font-size: 48px;
                            margin: 20px 0;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>👋 Account Deleted</h1>
                        </div>
                        <div class="content">
                            <p>Hi <strong>%s</strong>,</p>
                            <div class="goodbye">💔</div>
                            <p>Your <strong>SecPlus Prep</strong> account has been permanently deleted as requested.</p>
                            <div class="info-box">
                                <strong>What's been deleted:</strong><br>
                                ✓ Your account information<br>
                                ✓ All your quiz results and progress<br>
                                ✓ All verification tokens<br>
                                ✓ All password reset tokens
                            </div>
                            <p>We're sorry to see you go! If you change your mind, you're always welcome to create a new account and continue your Security+ certification journey.</p>
                            <p>Thank you for choosing SecPlus Prep for your certification preparation! We hope we helped you on your path to Security+ success! 🎯</p>
                            <p><em>If you didn't request this deletion, please contact our support team immediately.</em></p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2025 SecPlus Prep. All rights reserved.</p>
                            <p>This is an automated email, please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(username);
    }
}