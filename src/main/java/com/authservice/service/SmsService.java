package com.authservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import lombok.extern.slf4j.Slf4j;

/**
 * SmsService - Handles sending SMS messages via Twilio API
 * Used for phone number verification with 6 digit code
 */@Service
   @Slf4j
   public class SmsService {

    /**
     * Twilio account credentials from .env file
     * To Do: Recreate .env file for twilio. (application.yml reads from .env)
     * 
     * Twilio To Do: 
     * 1. Sign up for twilio account
     * 2. Go to console dashboard
     * 3. Copy account-sid, auth-token, and get phone number
     */ @Value("${twilio.account-sid}")
        private String accountSid;

        @Value("${twilio.auth-token}")
        private String authToken;

        @Value("${twilio.phone-number}")
        private String fromPhoneNumber;


        
}
