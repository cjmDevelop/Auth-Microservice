package com.authservice.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.authservice.dto.auth.AuthResponseDto;
import com.authservice.dto.auth.LoginRequestDto;
import com.authservice.dto.auth.RegisterRequestDto;
import com.authservice.dto.auth.VerificationRequestDto;
import com.authservice.service.AuthService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponseDto register(@RequestBody RegisterRequestDto request) {
       return authService.register(request);
    }

    @PostMapping("/verify-email")
    public AuthResponseDto verifyEmail(@RequestBody VerificationRequestDto verReqDto) {
        return authService.verifyEmail(verReqDto);
    }
    

    @PostMapping("/login")
    public AuthResponseDto login(@RequestBody LoginRequestDto request) {    
        return authService.login(request);
    }
    


    

    
    
}
