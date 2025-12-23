package com.authservice.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.authservice.dto.UserDto;
import com.authservice.dto.auth.AuthResponseDto;
import com.authservice.dto.auth.LoginRequestDto;
import com.authservice.dto.auth.RegisterRequestDto;
import com.authservice.dto.auth.VerificationRequestDto;
import com.authservice.repository.UserRepository;
import com.authservice.repository.VerificationTokenRepository;

import com.authservice.model.User;
import com.authservice.model.Role;
import com.authservice.model.AppSource;
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
    private final ResendEmailService emailService;
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
     * Development mode flag - auto-verifies users and skips email sending
     */
    @Value("${app.dev-mode:false}")
    private boolean devMode;

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

        // Determine the app source (from request or default)
        AppSource appSource = requestDto.getAppSource() != null ? requestDto.getAppSource() : AppSource.RANDOM_WRITES;
        log.info("Registration for app: {}", appSource);

        // Check if email exists for THIS APP
        Optional<User> existingUser = userRepository.findByEmailAndAppSource(requestDto.getEmail(), appSource);
        if (existingUser.isPresent()) {
            throw new RuntimeException("Email already registered for this application");
        }

        // Check if phone number exists for THIS APP
        if (requestDto.getPhoneNumber() != null) {
            Optional<User> existingPhone = userRepository.findByPhoneNumberAndAppSource(requestDto.getPhoneNumber(), appSource);
            if (existingPhone.isPresent()) {
                throw new RuntimeException("Phone number already registered for this application");
            }
        }

        // Creating new user with encrypted password
        User user = User.builder()
            .email(requestDto.getEmail())
            .password(passwordEncoder.encode(requestDto.getPassword()))
            .firstName(requestDto.getFirstName())
            .lastName(requestDto.getLastName())
            .phoneNumber(requestDto.getPhoneNumber())
            .role(Role.USER)
            .appSource(appSource)
            .emailVerified(false)
            .phoneVerified(false)
            .isEnabled(true)
            .build();

        // Saving user to database
        user = userRepository.save(user);
        log.info("User saved with ID: {}", user.getId());

        // DEV MODE: Auto-verify user and skip email
        if (devMode) {
            log.warn("🔧 DEV MODE: Auto-verifying user {} without email verification", user.getEmail());
            user.setEmailVerified(true);
            user = userRepository.save(user);

            // Generate JWT tokens for immediate login
            String accessToken = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            UserDto userDto = convertToDto(user);

            log.info("✅ DEV MODE: User {} auto-verified and logged in", user.getEmail());

            // Return response WITH tokens (user can login immediately)
            return AuthResponseDto.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getExpirationTime())
                    .user(userDto)
                    .build();
        }

        // PRODUCTION MODE: Generate and send email verification code
        String verificationCode = generateVerificationCode();
        saveVerificationToken(user, verificationCode, VerificationToken.VerificationType.EMAIL);

        // Sending verification email asynchronously
        emailService.sendVerificationEmail(
                user.getEmail(),
                verificationCode,
                user.getFirstName(),
                user.getAppSource());

        // Convert User entity to DTO
        UserDto userDto = convertToDto(user);

        // Return response without tokens
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
        log.info("Login attempt for user: {} (app: {})", request.getEmail(), request.getAppSource());

        // Authenticating user, incorrect credentials should throw an exception
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        // Instantiate 'user' object by finding user in database by email and appSource
        User user;
        if (request.getAppSource() != null && !request.getAppSource().isEmpty()) {
            try {
                AppSource appSource = AppSource.valueOf(request.getAppSource());
                user = userRepository.findByEmailAndAppSource(request.getEmail(), appSource)
                        .orElseThrow(() -> new RuntimeException("User not found for this application"));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid app source: " + request.getAppSource());
            }
        } else {
            // Backwards compatibility: if no appSource provided, use email only
            user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }

        if (!user.isEmailVerified()) {
            throw new RuntimeException("Email not verified. Please verify your email first.");
        }

        // Generating access & refresh JWT tokens
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

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
     * Refresh access token using refresh token
     *
     * Flow:
     * 1. Extract username from refresh token
     * 2. Load user from database
     * 3. Validate refresh token
     * 4. Generate new access token
     * 5. Return new access token with same refresh token
     *
     * @param refreshToken The refresh token
     * @return AuthResponseDto with new access token
     * @throws RuntimeException if refresh token is invalid or expired
     */
    public AuthResponseDto refreshAccessToken(String refreshToken) {
        log.info("Attempting to refresh access token");

        // Extract username from refresh token
        String username = jwtService.extractUsername(refreshToken);

        // Load user from database
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate refresh token
        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        // Generate new access token
        String newAccessToken = jwtService.generateToken(user);

        log.info("Access token refreshed successfully for: {}", user.getEmail());

        // Return new access token with same refresh token
        return AuthResponseDto.builder()
                .accessToken(newAccessToken)
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
        log.info("Email verification attempt for: {} (app: {})", request.getEmail(), request.getAppSource());

        // Finding user by email and appSource
        User user;
        if (request.getAppSource() != null && !request.getAppSource().isEmpty()) {
            try {
                AppSource appSource = AppSource.valueOf(request.getAppSource());
                user = userRepository.findByEmailAndAppSource(request.getEmail(), appSource)
                        .orElseThrow(() -> new RuntimeException("User not found for this application"));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid app source: " + request.getAppSource());
            }
        } else {
            // Backwards compatibility: if no appSource provided, use email only
            user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }

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
        emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName(), user.getAppSource());

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
         * @param appSourceStr - Application source string (optional, for backwards compatibility)
         * @throws RuntimeException if user not found or already verified
         */ @Transactional
            public void resendEmailVerification(String email, String appSourceStr) {
                log.info("Resending email verification to: {} for app: {}", email, appSourceStr);

                // Find user by email and appSource (if provided)
                User user;
                if (appSourceStr != null && !appSourceStr.isEmpty()) {
                    try {
                        AppSource appSource = AppSource.valueOf(appSourceStr);
                        user = userRepository.findByEmailAndAppSource(email, appSource)
                                .orElseThrow(() -> new RuntimeException("User not found for this application"));
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException("Invalid app source: " + appSourceStr);
                    }
                } else {
                    // Backwards compatibility: if no appSource provided, use email only
                    user = userRepository.findByEmail(email)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                }

                if(user.isEmailVerified()) {
                    throw new RuntimeException("Email already verified");
                }

                // DEV MODE: Skip resending email
                if (devMode) {
                    log.warn("🔧 DEV MODE: Skipping email resend for {}", email);
                    return;
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
                emailService.sendVerificationEmail(user.getEmail(), verificationCode, user.getFirstName(), user.getAppSource());

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
                .phoneNumber(user.getPhoneNumber()) 
                .role(user.getRole().name())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .build();

    }

}
