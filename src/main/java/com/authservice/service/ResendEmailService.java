package com.authservice.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    public ResendEmailService(
            @Value("${resend.api-key}") String apiKey,
            @Value("${resend.from-email}") String fromEmail
    ) {
        this.resendClient = new Resend(apiKey);
        this.fromEmail = fromEmail;
        log.info("✅ Resend Email Service initialized with from: {}", fromEmail);
    }

    /**
     * Send verification email with code to user
     * 
     * @Async - Runs in background thread, doesn't block API response
     */
    @Async("taskExecutor")
    public void sendVerificationEmail(String to, String code, String name) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("📧 Sending verification email to: {}", to);
            
            String htmlContent = buildVerificationEmail(name != null ? name : "there", code);

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(to)
                    .subject("Verify Your Email Address - Random Writes Random Lights")
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
     * Send welcome email after successful verification
     * 
     * @Async - Runs in background thread
     */
    @Async("taskExecutor")
    public void sendWelcomeEmail(String to, String name) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("📧 Sending welcome email to: {}", to);
            
            String htmlContent = buildWelcomeEmail(name != null ? name : "there");

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(to)
                    .subject("Welcome to Random Writes Random Lights! 🎉")
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
    public void sendPasswordResetEmail(String to, String username, String resetCode) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("📧 Sending password reset email to: {}", to);
            
            String htmlContent = buildPasswordResetEmail(username != null ? username : "there", resetCode);

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(to)
                    .subject("Reset Your Password - Random Writes Random Lights 🔐")
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
    public void sendPasswordChangedConfirmation(String to, String username) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("📧 Sending password changed confirmation to: {}", to);
            
            String htmlContent = buildPasswordChangedEmail(username != null ? username : "there");

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(fromEmail)
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
    // (Keep all your existing email template methods below - no changes needed)

    private String buildVerificationEmail(String name, String code) {
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

    private String buildWelcomeEmail(String name) {
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

    private String buildPasswordResetEmail(String username, String resetCode) {
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

    private String buildPasswordChangedEmail(String username) {
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

    /**
     * Send account deletion confirmation email
     *
     * @Async - Runs in background thread
     */
    @Async("taskExecutor")
    public void sendAccountDeletionConfirmation(String to, String username) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("📧 Sending account deletion confirmation to: {}", to);

            String htmlContent = buildAccountDeletionEmail(username != null ? username : "there");

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(fromEmail)
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
    private String buildAccountDeletionEmail(String username) {
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
}