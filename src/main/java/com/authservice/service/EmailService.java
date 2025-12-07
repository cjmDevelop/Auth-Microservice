package com.authservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Email service - Handles sending emails for verification and notifications.
 * Uses Spring Mail with SMTP configured in application.yml
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;
    
    /**
     * Send verification email with code to user.
     * @Async makes this run in background thread (non-blocking)
     * 
     * @param to User's email address 
     * @param code 6 digit verification code
     * @param name User's first name for personalization
     */
    @Async
     public void sendVerificationEmail(String to, String code, String name) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Verify Your Email Address");

            String htmlContent = buildVerificationEmail(name, code);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Verification email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to: {}", to, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
     }

    /**
     * Send welcome email after successful verification
     * 
     * @param to User's email address
     * @param name name User's first name
     */
    @Async
    public void sendWelcomeEmail(String to, String name) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Welcome to Our Service");//to do: Update this line according to frontend project.

            String htmlContent = buildWelcomeEmail(name);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcome email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send welcome email to: {}", to, e);
        }
    }

    /**
     * Build HTML email template for verification code
     * 
     * @param name User's first name
     * @param code 6 digit verification code
     * @return HTML string for email body
     */ private String buildVerificationEmail(String name, String code) {
        return """
                 <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { 
                        font-family: Arial, sans-serif; 
                        line-height: 1.6; 
                  
                    }
                    .container { 
                        max-width: 600px; 
                        margin: 20px auto; 
                        background: white;
                        border-radius: 10px;
                        overflow: hidden;
                        box-shadow: 0 0 20px rgba(0,0,0,0.1);
                    }
                    .header { 
                     
                        color: white; 
                        padding: 30px 20px; 
                        text-align: center; 
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 28px;
                    }
                    .content { 
                        padding: 40px 30px; 
                    }
                    .code-box { 
                        font-size: 36px; 
                        font-weight: bold; 
                       
                        text-align: center; 
                        padding: 25px; 
                    
              
                        border-radius: 8px; 
                        margin: 30px 0; 
                        letter-spacing: 8px;
                    }
                    .footer { 
                        text-align: center; 
                        padding: 20px; 
                    
                      
                        font-size: 12px; 
                    }
                    .warning {
                        
                        font-size: 14px;
                        margin-top: 20px;
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
                        <p>Thank you for registering! Please use the verification code below to verify your email address:</p>
                        <div class="code-box">%s</div>
                        <p>This code will expire in <strong>15 minutes</strong>.</p>
                        <p class="warning">⚠️ If you didn't create an account, please ignore this email.</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated message, please do not reply.</p>
                        <p>&copy; 2025 Auth Microservice. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
                """.formatted(name, code);
    }


    /**
     * Build HTML email template for welcome message
     * 
     * @param name User's first name
     * @return HTML string for email body
     */
    private String buildWelcomeEmail(String name) {
        return """
                   <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { 
                        font-family: Arial, sans-serif; 
                        line-height: 1.6; 
                      
                    }
                    .container { 
                        max-width: 600px; 
                        margin: 20px auto; 
                        background: white;
                        border-radius: 10px;
                        overflow: hidden;
                        box-shadow: 0 0 20px rgba(0,0,0,0.1);
                    }
                    .header { 
                       
                        color: white; 
                        padding: 30px 20px; 
                        text-align: center; 
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 28px;
                    }
                    .content { 
                        padding: 40px 30px; 
                    }
                    .button {
                        display: inline-block;
                        padding: 12px 30px;
                     
                        color: white;
                        text-decoration: none;
                        border-radius: 5px;
                        margin: 20px 0;
                    }
                    .footer { 
                        text-align: center; 
                        padding: 20px; 
                  
                        font-size: 12px; 
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
                        <p>Welcome to our service! Your email has been successfully verified.</p>
                        <p>You can now enjoy all the features of your account.</p>
                        <p>If you have any questions, feel free to contact our support team.</p>
                    </div>
                    <div class="footer">
                        <p>Thank you for joining us!</p>
                        <p>&copy; 2025 Auth Microservice. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
                """.formatted(name);
    }



    /**
 * Send password reset email with 6-digit code
 */
public void sendPasswordResetEmail(String to, String username, String resetCode) {
    try {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("Reset Your Password - Random Writes Random Lights");

        String htmlContent = buildPasswordResetEmail(username, resetCode);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    } catch (MessagingException e) {
        throw new RuntimeException("Failed to send password reset email", e);
    }
}

/**
 * Send confirmation email after password change
 */
public void sendPasswordChangedConfirmation(String to, String username) {
    try {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("Password Changed - Random Writes Random Lights");

        String htmlContent = buildPasswordChangedEmail(username);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    } catch (MessagingException e) {
        throw new RuntimeException("Failed to send password changed confirmation", e);
    }
}

/**
 * Build HTML content for password reset email
 */
private String buildPasswordResetEmail(String username, String resetCode) {
    return "<!DOCTYPE html>" +
           "<html>" +
           "<head>" +
           "    <style>" +
           "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
           "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
           "        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
           "        .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }" +
           "        .code-box { background: white; border: 2px dashed #667eea; border-radius: 10px; padding: 20px; text-align: center; margin: 20px 0; }" +
           "        .code { font-size: 32px; font-weight: bold; color: #667eea; letter-spacing: 5px; }" +
           "        .warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }" +
           "        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }" +
           "    </style>" +
           "</head>" +
           "<body>" +
           "    <div class='container'>" +
           "        <div class='header'>" +
           "            <h1>🔐 Password Reset Request</h1>" +
           "        </div>" +
           "        <div class='content'>" +
           "            <p>Hi " + username + ",</p>" +
           "            <p>We received a request to reset your password for your <strong>Random Writes Random Lights</strong> account.</p>" +
           "            <p>Use the code below to reset your password:</p>" +
           "            <div class='code-box'>" +
           "                <div class='code'>" + resetCode + "</div>" +
           "            </div>" +
           "            <div class='warning'>" +
           "                <strong>⏰ This code will expire in 15 minutes</strong>" +
           "            </div>" +
           "            <p>If you didn't request a password reset, please ignore this email or contact support if you have concerns.</p>" +
           "            <p>For security reasons, never share this code with anyone.</p>" +
           "        </div>" +
           "        <div class='footer'>" +
           "            <p>© 2024 Random Writes Random Lights. All rights reserved.</p>" +
           "            <p>This is an automated email, please do not reply.</p>" +
           "        </div>" +
           "    </div>" +
           "</body>" +
           "</html>";
}

/**
 * Build HTML content for password changed confirmation
 */
private String buildPasswordChangedEmail(String username) {
    return "<!DOCTYPE html>" +
           "<html>" +
           "<head>" +
           "    <style>" +
           "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
           "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
           "        .header { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
           "        .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }" +
           "        .success-box { background: #d4edda; border: 1px solid #c3e6cb; border-radius: 10px; padding: 20px; text-align: center; margin: 20px 0; }" +
           "        .warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }" +
           "        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }" +
           "    </style>" +
           "</head>" +
           "<body>" +
           "    <div class='container'>" +
           "        <div class='header'>" +
           "            <h1>✅ Password Successfully Changed</h1>" +
           "        </div>" +
           "        <div class='content'>" +
           "            <p>Hi " + username + ",</p>" +
           "            <div class='success-box'>" +
           "                <h2 style='color: #155724; margin: 0;'>Your password has been changed successfully!</h2>" +
           "            </div>" +
           "            <p>Your <strong>Random Writes Random Lights</strong> account password was recently changed.</p>" +
           "            <p>You can now log in with your new password.</p>" +
           "            <div class='warning'>" +
           "                <strong>⚠️ Didn't change your password?</strong><br>" +
           "                If you didn't make this change, please contact our support team immediately to secure your account." +
           "            </div>" +
           "        </div>" +
           "        <div class='footer'>" +
           "            <p>© 2024 Random Writes Random Lights. All rights reserved.</p>" +
           "            <p>This is an automated email, please do not reply.</p>" +
           "        </div>" +
           "    </div>" +
           "</body>" +
           "</html>";
}

}
