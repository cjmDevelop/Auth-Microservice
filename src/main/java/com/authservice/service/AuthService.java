package com.authservice.service;

import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.authservice.dto.AuthResponseDto;
import com.authservice.dto.LoginRequestDto;
import com.authservice.dto.RegisterRequestDto;
import com.authservice.dto.UserDto;
import com.authservice.dto.VerificationRequestDto;
import com.authservice.repository.UserRepository;
import com.authservice.repository.VerificationTokenRepository;

import com.authservice.model.User;
import com.authservice.model.Role;
import com.authservice.model.VerificationToken;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AuthService - Core authentication and authorization service
 * Handles user registration, login, email/SMS verification
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    /**
     * Injected dependencies via Lombok @RequiredArgsConstructor
     * All 'final' fields are automatically injected by Spring
     */
    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final EmailService emailService;
    private final SmsService smsService;
    private final AuthenticationManager authenticationManager;

    /**
     * Should be a 6 digit verification code (application.yml)
     */
    @Value("${app.verification.code-length}")
    private int codeLength;

    /**
     * Should be set at 15 minutes (application.yml)
     */
    @Value("${app.verification.expiration-minutes}")
    private int expirationMinutes;

    /**
     * Register a new user account
     * 
     * Flow:
     * 1. Check if email already exists
     * 2. Create user with encrypted password
     * 3. Generate verification code
     * 4. Send verification email
     * 5. Return success response (no JWT, still must verify)
     * 
     * @param request Registration details (email, password, name, phone)
     * @return AuthResponse with user info (no tokens until verified)
     * @throws RuntimeException if email already exists
     */
    @Transactional
    public AuthResponseDto register(RegisterRequestDto requestDto) {
        log.info("Registering new user: {}", requestDto.getEmail());

        // Checking if user already exist
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Check if phone number exists
        if (requestDto.getPhoneNumber() != null &&
                userRepository.existsByPhoneNumber(requestDto.getPhoneNumber())) {
            throw new RuntimeException("Phone number already registered");
        }

        // Creating new user with encrypted password
        User user = User.builder()
                .email(requestDto.getEmail())
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .firstName(requestDto.getFirstName())
                .lastName(requestDto.getLastName())
                .phoneNumber(requestDto.getPhoneNumber())
                .role(Role.USER)
                .emailVerified(false)
                .phoneVerified(false) //red error under user: The local variable user may not have been initializedJava(536870963)
                .isEnabled(true)
                .build();

        // Saving user to database
        user = userRepository.save(user);
        log.info("User saved with ID: {}", user.getId());

        // Generate and send email verification code
        // TO DO: define generateVerificationCode() & saveVerificationToken()
        String verificationCode = generateVerificationCode();
        saveVerificationToken(user, verificationCode, VerificationToken.VerificationType.EMAIL);

        // Sending verification email asynchronously
        emailService.sendVerificationEmail(
                user.getEmail(),
                verificationCode,
                user.getFirstName());

        // Convert User entity to DTO
        // TO DO: define convertToDto()
        UserDto userDto = convertToDto(user);

        // Return response without tokens
        // TO DO: Explain what Bearer is / is this in header? what are headers?
        return AuthResponseDto.builder()
                .user(userDto)
                .tokenType("Bearer")
                .build();
    }

    /**
     * Login existing user
     * 
     * Flow:
     * 1. Authenticate credentials
     * 2. Check if email is verified
     * 3. Generate JWT access token
     * 4. Generate refresh token
     * 5. Return tokens + user info
     * 
     * @param request Request login credentials (email, password)
     * @return AuthResponseDto with JWT tokens and user info
     * @throws RuntimeException
     */
    public AuthResponseDto login(LoginRequestDto request) {
        log.info("Login attempt for user: {}", request.getEmail());

        // Authenticating user, incorrect credentials should throw an exception
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        // Instantiate 'user' object by finding user in database by email,
        // will throw exception is user not found, or email is not verified.
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!user.isEmailVerified()) {
            throw new RuntimeException("Email not verified. Please verify your email first.");
        }

        // Generating access & refresh JWT tokens
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateToken(user);

        log.info("User logged in successfully: {}", user.getEmail());

        // Return tokens and user info
        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .user(convertToDto(user))
                .build();
    }

    /**
     * Verifying email with code sent via email
     * 
     * Flow:
     * 1. Find user by email
     * 2. Find verification token
     * 3. Check if code matches and not expired
     * 4. Mark email as verified
     * 5. Delete verification token
     * 6. Send welcome email
     * 7. Return JWT tokens (user can now login)
     * 
     * @param request verification code and email
     * @return AuthResponseDto with JWT tokens
     * @throws RuntimeException if code invalid/expired
     */
    @Transactional
    public AuthResponseDto verifyEmail(VerificationRequestDto request) {
        log.info("Email verification attempt for: {}", request.getEmail());

        // Finding user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Find verification token
        VerificationToken token = tokenRepository.findByCodeAndUserAndType(
                request.getCode(),
                user,
                VerificationToken.VerificationType.EMAIL)
                .orElseThrow(() -> new RuntimeException("Invalid verification code"));

        // Checking if token is expired
        if (token.isExpired()) {
            throw new RuntimeException("Verification code expired. Please request new code.");
        }

        // Checking if token is already verified
        if (token.isVerified()) {
            throw new RuntimeException("Email already verified");
        }

        // Marking user as verified & setting token stamped time
        user.setEmailVerified(true);
        token.setVerifiedAt(LocalDateTime.now());

        // Saving user and token to database? didn't we already do this in register()
        // method?
        userRepository.save(user);
        tokenRepository.save(token);

        log.info("Email verified successfully for: {}", user.getEmail());

        // Sending Welcome email
        emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName());

        // Generating JWT tokens inorder for user to login
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .user(convertToDto(user))
                .build();

    }

    /**
     * Send SMS verification code to user's phone
     * 
     * @param email (user's email)
     * @throws RuntimeException if user not found or phone not provided
     */
    @Transactional
    public void sendPhoneVerification(String email) {
        log.info("Sending phone verification to user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getPhoneNumber() == null) {
            throw new RuntimeException("Phone number not provided");
        }

        // Generate and save verification code
        String verificationCode = generateVerificationCode();
        saveVerificationToken(user, verificationCode, VerificationToken.VerificationType.PHONE);

        // Send SMS
        smsService.sendVerificationSms(user.getPhoneNumber(), verificationCode); //The method sendVerificationSms(String, String) is undefined for the type SmsServiceJava(67108964)

        log.info("Phone verification code sent to: {}", user.getPhoneNumber());
    }

    /**
     * Verify phone number with SMS code
     * 
     * @param request Verification code and email
     * @throws RuntimeException if code is invalid or expired
     */
    @Transactional
    public void verifyPhone(VerificationRequestDto request) {
        log.info("Phone verification attempt for: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        VerificationToken token = tokenRepository
                .findByCodeAndUserAndType(
                        request.getCode(), user,
                        VerificationToken.VerificationType.PHONE)
                .orElseThrow(() -> new RuntimeException("Invalid verification code"));

        if (token.isExpired()) {
            throw new RuntimeException("Verification code expired");
        }

        if (token.isVerified()) {
            throw new RuntimeException("Phone already verified");
        }

        // Marking phone number as verified
        user.setPhoneVerified(true);
        token.setVerifiedAt(LocalDateTime.now());

        userRepository.save(user);
        tokenRepository.save(token);

        log.info("Phone verified successfully for: {}", user.getEmail());
    }

    /**
         * Resend email verification code
         * 
         * @param email - User's email
         * @throws RuntimeException if user not found or already verified
         */ @Transactional
            public void resendEmailVerification(String email) {
                log.info("Resending email verification to: {}", email);

                User user = userRepository.findByEmail(email)
                            .orElseThrow(() -> new RuntimeException("User not found"));

                if(user.isEmailVerified()) {
                    throw new RuntimeException("Email already verified");
                }

                //Delete old tokens
                tokenRepository.findByUserAndTypeAndVerifiedAtIsNull(
                            user, 
                            VerificationToken.VerificationType.EMAIL
                            ).ifPresent(tokenRepository::delete);

                //Generate new code
                String verificationCode = generateVerificationCode();
                saveVerificationToken(user, verificationCode, VerificationToken.VerificationType.EMAIL);

                //Resend email
                emailService.sendVerificationEmail(user.getEmail(), verificationCode, user.getFirstName());
           
                log.info("Verification email resent to: {}", email);
            
            }

    /**
     * Generate random verification code
     * 6 digits, set in application.yml
     * 
     * @return Random numeric code
     */
    private String generateVerificationCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < codeLength; i++) {
            code.append(random.nextInt(10)); // Random digit 0 - 9
        }
        return code.toString();
    }

             /**
               * Save verification token to database
               * 
               * @param user User entity
               * @param code Verification code
               * @param type EMAIL or PHONE
               */ private void saveVerificationToken(User user, String code, VerificationToken.VerificationType type) {
                  VerificationToken token = VerificationToken.builder()
                                            .code(code)
                                            .user(user)
                                            .type(type)
                                            .expiresAt(LocalDateTime.now().plusMinutes(expirationMinutes))
                                            .build();

                  tokenRepository.save(token);
                  log.debug("Verification token saved for user: {}", user.getEmail());
               }

    /**
     * Convert User entity to UserDto
     * Excludes sensitive data like password
     * 
     * @param user User entity
     * @return UserDto for API responses
     */
    private UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber()) //The method phoneNumber(String) in the type UserDto.UserDtoBuilder is not applicable for the arguments (boolean)Java(67108979)
                .role(user.getRole().name())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .build();

    }

}
