package com.authservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * SmsService - Handles sending SMS messages via Twilio API
 * Used for phone number verification with 6 digit code
 */
@Service
@Slf4j
public class SmsService {

  private final EmailService emailService;

  /**
   * Twilio account credentials from .env file
   * To Do: Recreate .env file for twilio. (application.yml reads from .env)
   * 
   * Twilio To Do:
   * 1. Sign up for twilio account
   * 2. Go to console dashboard
   * 3. Copy account-sid, auth-token, and get phone number
   */
  @Value("${twilio.account-sid}")
  private String accountSid;

  @Value("${twilio.auth-token}")
  private String authToken;

  @Value("${twilio.phone-number}")
  private String fromPhoneNumber;

  SmsService(EmailService emailService) {
    this.emailService = emailService;
  }

  /**
   * Initialize Twilio SDK with credentials
   * 
   * @PostConstruct runs automatically after this bean is created
   *                This ensures Twilio is configured before any SMS is sent
   */
  @PostConstruct
  public void initTwilio() {
    Twilio.init(accountSid, authToken);
    log.info("Twilio SMS service initialized");
  }

  /**
   * Send SMS verification code to user's phone
   * @Async makes this run in background thread and doesn't block API responses. 
   * 
   * @param toPhoneNumber User's phone number and must include country code, e.g., +1234567890
   * @param code is a 6 digit code set in application.yml
   * 
   * Phone number format:
   * Must start with + and country code 
   * example: +1234567890 (US number)
   * example: +447911123456 (UK number)
   */
  @Async
  public void sendVerificationSms(String toPhoneNumber, String code) {
    try {
      /**
       * Build SMS message body
       * The shorter the better, sms may have approx. 160 character limit
       */
      String messageBody = String.format(
        "Your verification code is: %s\n\nThis code expires in 10 minutes.", code);

      /**
       * Send SMS using Twilio API
       * 
       * Message.creator() requires:
       * 1. To: The Recipient's phone number
       * 2. From: Your twilio phone number 
       * 3. Body: the message's text
       * 4. .create() sends the SMS
       */
      Message message = Message.creator(
        new PhoneNumber(toPhoneNumber), 
        new PhoneNumber(fromPhoneNumber), 
        messageBody).create();

      /**
       * Log success with message SID (unique identifier)
       * SID can be used to track delivery status in Twilio dashboard
       */
      log.info("SMS sent successfully to: {} with SID: {}", toPhoneNumber, message.getSid());
    } catch (Exception e) {
      /**
       * Log's error but wont crash cause application to crash
       * User can still try email verification if SMS fails
       */
      log.error("Failed to send SMS to: {}", toPhoneNumber, e);
      throw new RuntimeException("Failed to send SMS verification code", e);
    }
  }

  /**
   * Send a generic SMS, this may later turn into an extra feature that requires phone verification of some kind.
   * 
   * @param toPhoneNumber Recipient's/user's phone number
   * @param messageText The message to send
   * 
   * Use cases:
   * Password reset notifications
   * Account alerts
   * Two-factor authentication
   */
  @Async
  public void sendSms(String toPhoneNumber, String messageText) {
    try {
      Message message = Message.creator(
        new PhoneNumber(toPhoneNumber), 
        new PhoneNumber(fromPhoneNumber), 
        messageText).create();
      log.info("SMS sent to: {} with SID: {}", toPhoneNumber, message.getSid());
    } catch (Exception e) {
      log.error("Failed to send SMS to: {}", toPhoneNumber, e);
    }
  }
}