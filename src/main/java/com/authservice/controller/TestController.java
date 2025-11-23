package com.authservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.authservice.service.EmailService;


import lombok.RequiredArgsConstructor;



@RestController
@RequiredArgsConstructor
public class TestController {

    private final EmailService emailService;

    @GetMapping("/test/email")
    public String testEmailService() {
        emailService.sendVerificationEmail("jr87.dev@gmail.com", "978462", "testName");
        return "Email sent!";
    }
    
    
}
