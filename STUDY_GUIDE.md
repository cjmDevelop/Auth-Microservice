# Authentication Microservice - Complete Study Guide

## 1. PROJECT OVERVIEW

**Project Name:** Authentication Microservice
**Framework:** Spring Boot 3.5.7
**Language:** Java 21
**Build Tool:** Maven
**Database:** PostgreSQL

**Key Features:**
- JWT-based authentication (stateless)
- User registration and email verification
- Password reset flow with 6-digit codes
- Multi-app support (RANDOM_WRITES, SECPLUS_PREP)
- Email service integration (Resend, Gmail SMTP, AWS SES)
- SMS verification (Twilio)
- Notes management system
- Quiz results tracking
- Account deletion with cascade delete
- Async email processing for performance

---

## 2. OVERALL ARCHITECTURE & FLOW

```
HTTP Request
    ↓
JwtAuthenticationFilter (validates JWT token)
    ↓
SecurityFilterChain (checks authorization)
    ↓
Controller (REST endpoint)
    ↓
Service (business logic)
    ↓
Repository (database access)
    ↓
Database (PostgreSQL)
```

**Request Flow Example (Registration):**
1. Client sends POST /api/auth/register with email, password, name
2. AuthController.register() receives request
3. AuthService.register() validates and creates user
4. PasswordEncoder encrypts password with BCrypt
5. User saved to database via UserRepository
6. VerificationToken created and saved
7. Email sent asynchronously via ResendEmailService
8. Response returned immediately (no wait for email)

---

## 3. PROJECT STRUCTURE

```
src/main/java/com/authservice/
├── config/                    # Security & async configuration
│   ├── AsyncConfig.java       # Thread pool for async tasks
│   ├── CorsConfig.java        # CORS settings for multiple domains
│   ├── JwtAuthenticationFilter.java  # JWT validation filter
│   └── SecurityConfig.java    # Spring Security configuration
├── controller/                # REST endpoints
│   ├── AuthController.java    # Register, login, email verification
│   ├── PasswordResetController.java  # Password reset endpoints
│   ├── NoteController.java    # CRUD operations for notes
│   ├── QuizResultController.java  # Quiz result management
│   └── HealthCheckController.java
├── dto/                       # Data Transfer Objects
│   ├── UserDto.java           # User response (no password)
│   ├── auth/
│   │   ├── RegisterRequestDto.java
│   │   ├── LoginRequestDto.java
│   │   ├── AuthResponseDto.java
│   │   ├── VerificationRequestDto.java
│   │   ├── PasswordResetRequestDto.java
│   │   ├── PasswordResetVerifyDto.java
│   │   ├── PasswordUpdateDto.java
│   │   └── AccountDeletionDto.java
│   ├── notes/
│   │   ├── CreateNoteRequestDto.java
│   │   ├── NoteResponseDto.java
│   │   └── UpdateNoteRequestDto.java
│   └── quiz/
│       ├── QuizResultRequestDto.java
│       └── QuizResultResponseDto.java
├── model/                     # JPA Entities
│   ├── User.java              # Main user entity
│   ├── Role.java              # Enum: USER, ADMIN, MODERATOR
│   ├── AppSource.java         # Enum: RANDOM_WRITES, SECPLUS_PREP
│   ├── VerificationToken.java # Email/phone verification tokens
│   ├── PasswordResetToken.java # Password reset tokens
│   ├── Note.java              # User notes/ideas
│   └── QuizResult.java        # Quiz attempt results
├── repository/                # Data Access Layer
│   ├── UserRepository.java
│   ├── VerificationTokenRepository.java
│   ├── PasswordResetTokenRepository.java
│   ├── NoteRepository.java
│   └── QuizResultRepository.java
├── service/                   # Business Logic Layer
│   ├── AuthService.java       # Core authentication service
│   ├── CustomUserDetailsService.java  # Load user for Spring Security
│   ├── JWTService.java        # Create & validate JWT tokens
│   ├── PasswordResetService.java  # Password reset logic
│   ├── ResendEmailService.java  # Email sending (async)
│   ├── SmsService.java        # Twilio SMS integration
│   ├── NoteService.java       # Note CRUD operations
│   ├── QuizResultService.java # Quiz result management
│   └── AccountDeletionService.java  # Account deletion logic
└── AuthMicroserviceApplication.java  # Main entry point
```

---

## 4. DATA MODELS & DATABASE RELATIONSHIPS

### User Entity (Core User Account)
**File:** `src/main/java/com/authservice/model/User.java`

```java
@Entity @Table(name = "users")
Fields:
- id (PK, auto-increment)
- email (UNIQUE with app_source)
- password (BCrypt encrypted)
- firstName, lastName, phoneNumber
- role (Enum: USER, ADMIN, MODERATOR)
- appSource (Enum: RANDOM_WRITES, SECPLUS_PREP)
- emailVerified (boolean, default=false)
- phoneVerified (boolean, default=false)
- isEnabled (boolean, default=true)
- createdAt, updatedAt (timestamps)

Relationships (Cascade DELETE):
- OneToMany → PasswordResetToken (orphanRemoval=true)
- OneToMany → VerificationToken (orphanRemoval=true)
- OneToMany → Note (orphanRemoval=true)
- OneToMany → QuizResult (orphanRemoval=true)
```

**Key Points:**
- Implements Spring Security's `UserDetails` interface
- `@PrePersist` sets `createdAt`, `updatedAt`, and default role
- `@PreUpdate` updates `updatedAt` timestamp
- Cascade delete ensures no orphaned data when user deleted

### VerificationToken Entity (Email/Phone Verification)
**File:** `src/main/java/com/authservice/model/VerificationToken.java`

```java
@Entity @Table(name = "verification_tokens")
Fields:
- id (PK)
- code (String, 6 digits)
- user_id (FK → User)
- type (Enum: EMAIL, PHONE)
- expiresAt (15 minutes from creation)
- verifiedAt (null until verified)
- createdAt (timestamp)

Methods:
- isExpired() → LocalDateTime.now() > expiresAt
- isVerified() → verifiedAt != null
```

### PasswordResetToken Entity (Password Reset)
**File:** `src/main/java/com/authservice/model/PasswordResetToken.java`

```java
@Entity @Table(name = "password_reset_tokens")
Fields:
- id (PK)
- user_id (FK → User)
- code (String, 6 digits)
- expiryDate (15 minutes)
- used (boolean, prevents reuse)
- createdAt (timestamp)

Methods:
- isExpired() → LocalDateTime.now() > expiryDate
- isValid() → !used AND !isExpired()
```

### Note Entity (User Notes)
**File:** `src/main/java/com/authservice/model/Note.java`

```java
@Entity @Table(name = "notes")
Fields:
- id (PK)
- user_id (FK → User)
- content (TEXT, required)
- createdAt (immutable)
- updatedAt (auto-updated)
- deletedAt (nullable)
```

### QuizResult Entity (Quiz Attempts)
**File:** `src/main/java/com/authservice/model/QuizResult.java`

```java
@Entity @Table(name = "quiz_results")
Fields:
- id (PK)
- user_id (FK → User)
- quizDomain (String: "1.1", "2.3", etc.)
- totalQuestions (Integer)
- correctAnswers (Integer)
- percentage (Double, calculated)
- passed (Boolean, >= 70%)
- timeTakenSeconds (Integer, optional)
- isTimed (Boolean)
- completedAt (timestamp)

Auto-calculated on persist/update:
- percentage = (correctAnswers / totalQuestions) * 100
- passed = percentage >= 70.0
```

---

## 5. AUTHENTICATION FLOW

### 5A. Registration Flow

**Endpoint:** `POST /api/auth/register`
**File:** `src/main/java/com/authservice/controller/AuthController.java:register()`

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+12025551234",
  "appSource": "RANDOM_WRITES"
}
```

**Flow:**
1. **Validation** - Email format, password min 8 chars
2. **Check Existence** - `findByEmailAndAppSource()` prevents duplicates
3. **Create User** - Build User with BCrypt-encoded password
4. **Save to Database** - User saved, timestamps set via @PrePersist
5. **Generate Verification Code** - 6 random digits
6. **Create VerificationToken** - Expires in 15 minutes
7. **Dev Mode Check** - If `app.dev-mode=true`:
   - Auto-verify user (`emailVerified=true`)
   - Generate JWT tokens immediately
   - Return tokens in response
8. **Production Mode** - Send verification email asynchronously
9. **Return Response** - UserDto (no password) + message

**Response (Production):**
```json
{
  "tokenType": "Bearer",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "firstName": "John",
    "emailVerified": false
  }
}
```

### 5B. Email Verification Flow

**Endpoint:** `POST /api/auth/verify-email`
**File:** `src/main/java/com/authservice/service/AuthService.java:verifyEmail()`

**Request:**
```json
{
  "email": "user@example.com",
  "code": "123456",
  "appSource": "RANDOM_WRITES"
}
```

**Flow:**
1. Find User by email and appSource
2. Find VerificationToken by code, user, and type=EMAIL
3. Validate token (not expired, not already verified)
4. Mark verified: `user.emailVerified = true`, `token.verifiedAt = now()`
5. Save changes to database
6. Send welcome email (async)
7. Generate JWT tokens (access + refresh)
8. Return tokens + user info

**Response:**
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": 1,
    "email": "user@example.com",
    "emailVerified": true
  }
}
```

### 5C. Login Flow

**Endpoint:** `POST /api/auth/login`
**File:** `src/main/java/com/authservice/service/AuthService.java:login()`

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123",
  "appSource": "RANDOM_WRITES"
}
```

**Flow:**
1. **Authenticate** - `AuthenticationManager.authenticate()`
   - Calls `CustomUserDetailsService.loadUserByUsername()`
   - Compares password with BCrypt
   - Throws `BadCredentialsException` if mismatch
2. **Check Email Verified** - Must have `emailVerified=true`
3. **Generate Tokens** - Via JWTService
4. **Return Response** - Tokens + user info

### 5D. Token Refresh Flow

**Endpoint:** `POST /api/auth/refresh`
**File:** `src/main/java/com/authservice/service/AuthService.java:refreshAccessToken()`

**Request:**
```json
{
  "refreshToken": "eyJhbGc..."
}
```

**Flow:**
1. Extract username from refresh token
2. Load user from database
3. Validate token (signature, expiration)
4. Generate new access token (24h)
5. Return new access token + same refresh token

---

## 6. PASSWORD RESET FLOW

### Step 1: Request Reset
**Endpoint:** `POST /api/auth/forgot-password`
**File:** `src/main/java/com/authservice/service/PasswordResetService.java:initiatePasswordReset()`

**Request:**
```json
{
  "email": "user@example.com",
  "appSource": "RANDOM_WRITES"
}
```

**Flow:**
1. Find user by email + appSource
2. Verify email is already verified
3. Delete old reset tokens for user
4. Generate 6-digit code
5. Create PasswordResetToken (expires 15 min)
6. Send email asynchronously
7. Return success message

### Step 2: Verify Code
**Endpoint:** `POST /api/auth/verify-reset-code`
**File:** `src/main/java/com/authservice/service/PasswordResetService.java:verifyResetCode()`

**Request:**
```json
{
  "email": "user@example.com",
  "code": "123456",
  "appSource": "RANDOM_WRITES"
}
```

**Flow:**
1. Find user
2. Find reset token by user + code
3. Check if expired or used
4. Return validation result

### Step 3: Reset Password
**Endpoint:** `POST /api/auth/reset-password`
**File:** `src/main/java/com/authservice/service/PasswordResetService.java:resetPassword()`

**Request:**
```json
{
  "email": "user@example.com",
  "code": "123456",
  "newPassword": "NewPassword123",
  "appSource": "RANDOM_WRITES"
}
```

**Flow:**
1. Find and validate reset token
2. Validate new password (min 8 chars)
3. Update user password (BCrypt)
4. Mark token as used
5. Send confirmation email (async)
6. Return success

---

## 7. SECURITY IMPLEMENTATION

### JWT (JSON Web Tokens)
**File:** `src/main/java/com/authservice/service/JWTService.java`

**Token Structure:** `HEADER.PAYLOAD.SIGNATURE`

**Header:**
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**Payload (Claims):**
```json
{
  "sub": "user@example.com",
  "iat": 1234567890,
  "exp": 1234654290
}
```

**Configuration:**
- **Access Token:** 24 hours (86400000 ms)
- **Refresh Token:** 7 days (604800000 ms)
- **Algorithm:** HMAC-SHA256
- **Secret:** Base64-encoded 256-bit key

**Token Validation Flow:**
**File:** `src/main/java/com/authservice/config/JwtAuthenticationFilter.java`

1. Extract Authorization header
2. Extract JWT from "Bearer " prefix
3. Extract username (email) from token
4. Load user from database
5. Validate token signature + expiration
6. Set authentication in SecurityContext
7. Request proceeds to controller

### Password Security

**BCrypt Implementation:**
**File:** `src/main/java/com/authservice/config/SecurityConfig.java`

```java
PasswordEncoder.encode(password)
- Generates random salt
- Hashes password with BCrypt (10 rounds)
- Returns encrypted string

passwordEncoder.matches(rawPassword, hashedPassword)
- Extracts salt from stored hash
- Hashes input with same salt
- Compares to stored hash
```

### Spring Security Configuration
**File:** `src/main/java/com/authservice/config/SecurityConfig.java`

**SecurityFilterChain:**
- OPTIONS requests → permitAll() (CORS preflight)
- Health checks (/health, /ping) → permitAll()
- Auth endpoints → permitAll()
- All other endpoints → authenticated()

**Filter Order:**
1. JwtAuthenticationFilter (validates JWT)
2. SecurityFilterChain (checks authorization)
3. Controller (business logic)

**CSRF:** Disabled (safe for JWT)
**Session Management:** Stateless

### CORS Configuration
**File:** `src/main/java/com/authservice/config/CorsConfig.java`

- **Allowed origins:** localhost:3000/3001/5173, secplus-prep.com, randomwritesrandomlights.com
- **Allowed methods:** GET, POST, PUT, DELETE, OPTIONS
- **Allowed headers:** *
- **Credentials:** true

---

## 8. MULTI-APP SUPPORT (AppSource)

**Enum:** `src/main/java/com/authservice/model/AppSource.java`

**Two Applications:**
1. **RANDOM_WRITES** - randomwritesrandomlights.com (notes app)
2. **SECPLUS_PREP** - secplus-prep.com (Security+ exam prep)

**Implementation:**

**Database Unique Constraint:**
```java
@UniqueConstraint(columnNames = {"email", "app_source"})
```
- Same email can register for both apps
- Separate accounts per app

**Repository Queries:**
```java
findByEmailAndAppSource(email, appSource)
findByPhoneNumberAndAppSource(phone, appSource)
```

**Email Service Routing:**
**File:** `src/main/java/com/authservice/service/ResendEmailService.java`

- Different email templates per app
- Different "from" addresses
- Different SMTP providers:
  - RANDOM_WRITES → Resend API
  - SECPLUS_PREP → Amazon SES SMTP

---

## 9. EMAIL SERVICES

### Email Service Architecture
**File:** `src/main/java/com/authservice/service/ResendEmailService.java`

**Three Email Providers:**
1. **Resend** (Primary for RANDOM_WRITES)
   - API-based
   - Modern email service
2. **Amazon SES SMTP** (For SECPLUS_PREP)
   - SMTP-based
   - AWS integration
3. **Gmail SMTP** (Local testing)
   - SMTP-based
   - Development use

**Async Processing:**
**File:** `src/main/java/com/authservice/config/AsyncConfig.java`

```java
@Async("taskExecutor")
public void sendVerificationEmail(...) {
    // Runs in background thread
    // API returns immediately
}
```

**Thread Pool Configuration:**
- Core pool size: 2 threads
- Max pool size: 5 threads
- Queue capacity: 100 tasks

### Email Types

1. **Verification Email** - 6-digit code, expires 15 min
2. **Welcome Email** - After verification
3. **Password Reset Email** - 6-digit code, expires 15 min
4. **Password Changed Email** - Confirmation
5. **Account Deletion Email** - Lists deleted data

---

## 10. NOTES FEATURE

**File:** `src/main/java/com/authservice/service/NoteService.java`

### Note Endpoints

**GET /api/notes** - Get all user's notes
```java
getUserNotes() → List<Note>
- Gets current user from SecurityContext
- Returns all user's notes
```

**POST /api/notes** - Create new note
```java
createNote(String content) → Note
- Validates content not empty
- Creates note for current user
```

**PUT /api/notes/{id}** - Update note
```java
updateNote(Long noteId, String newContent) → Note
- Security check: note belongs to user
- Updates content and timestamp
```

**DELETE /api/notes/{id}** - Delete note
```java
deleteNote(Long noteId)
- Security check: note belongs to user
- Hard delete from database
```

**Security:**
- All endpoints require JWT authentication
- Service extracts user from SecurityContext
- Queries include both note ID and user ID
- Prevents access to other users' notes

---

## 11. QUIZ RESULTS FEATURE

**File:** `src/main/java/com/authservice/service/QuizResultService.java`

### Quiz Endpoints

**POST /api/quiz-results** - Submit quiz result
```java
saveQuizResult(email, QuizResultRequestDto) → QuizResultResponseDto
- Percentage auto-calculated
- Passed auto-set (>= 70%)
```

**GET /api/quiz-results** - Get all quiz results
```java
getUserQuizResults(email) → List<QuizResultResponseDto>
- All attempts by user
- Ordered by newest first
```

**GET /api/quiz-results/domain/{domain}** - Get results for specific domain
```java
getUserQuizResultsByDomain(email, domain) → List<QuizResultResponseDto>
- All attempts for domain (e.g., "1.1")
```

**GET /api/quiz-results/domain/{domain}/best** - Get best score
```java
getBestResultByDomain(email, domain) → QuizResultResponseDto
- Highest percentage for domain
```

**GET /api/quiz-results/statistics** - Get user statistics
```java
getUserStatistics(email) → QuizStatisticsDto
- Total attempts
- Passed count
- Failed count
```

**Quiz Logic:**
```java
Percentage = (correctAnswers / totalQuestions) * 100
Passed = percentage >= 70.0  // CompTIA passing score
```

---

## 12. ACCOUNT DELETION FEATURE

**File:** `src/main/java/com/authservice/service/AccountDeletionService.java`

**Endpoint:** `DELETE /api/auth/account`

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123",
  "reason": "No longer needed"
}
```

**Flow:**
1. Verify authentication
2. Find user by email
3. Verify password correct
4. Save user info for email
5. Delete user from database
6. Cascade delete triggers:
   - All PasswordResetTokens
   - All VerificationTokens
   - All Notes
   - All QuizResults
7. Send confirmation email (async)
8. Return success

**Security:**
- Requires JWT authentication
- Password verification required
- Hard delete (allows email reuse)
- GDPR compliant

---

## 13. KEY SERVICES DEEP DIVE

### AuthService.java
**File:** `src/main/java/com/authservice/service/AuthService.java`

**Main Methods:**
- `register()` - Create user, send verification
- `login()` - Authenticate, return tokens
- `verifyEmail()` - Validate code, mark verified
- `refreshAccessToken()` - Generate new access token
- `sendPhoneVerification()` - SMS verification
- `verifyPhone()` - Validate phone code
- `resendEmailVerification()` - New verification code

### JWTService.java
**File:** `src/main/java/com/authservice/service/JWTService.java`

**Main Methods:**
- `generateToken()` - Create access token (24h)
- `generateRefreshToken()` - Create refresh token (7d)
- `extractUsername()` - Decode token, get email
- `isTokenValid()` - Validate signature + expiration
- `extractExpiration()` - Get expiration date

### PasswordResetService.java
**File:** `src/main/java/com/authservice/service/PasswordResetService.java`

**Main Methods:**
- `initiatePasswordReset()` - Generate code, send email
- `verifyResetCode()` - Validate code
- `resetPassword()` - Update password, mark used
- `resendResetCode()` - New reset code
- `cleanupExpiredTokens()` - Scheduled cleanup

### ResendEmailService.java
**File:** `src/main/java/com/authservice/service/ResendEmailService.java`

**All methods are @Async:**
- `sendVerificationEmail()`
- `sendWelcomeEmail()`
- `sendPasswordResetEmail()`
- `sendPasswordChangedConfirmation()`
- `sendAccountDeletionConfirmation()`

**Features:**
- Dev mode skips sending
- Routes to correct provider
- Branded templates per app
- Async execution
- Error logging

---

## 14. CONFIGURATION FILES

### application.yml (Dev)
**File:** `src/main/resources/application.yml`

```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
```

### application-dev.yml
**File:** `src/main/resources/application-dev.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/authservice_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate.ddl-auto: update
    show-sql: true
  mail:
    host: smtp.gmail.com
    port: 587

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000        # 24 hours
  refresh-expiration: 604800000  # 7 days

app:
  dev-mode: ${DEV_MODE:false}
  verification:
    code-length: 6
    expiration-minutes: 15
```

### application-prod.yml
**File:** `src/main/resources/application-prod.yml`

```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
  jpa:
    show-sql: false
  mail:
    host: email-smtp.us-east-2.amazonaws.com

app:
  dev-mode: false
```

---

## 15. API ENDPOINTS REFERENCE

### Auth Endpoints
```
POST   /api/auth/register              Register new user
POST   /api/auth/login                 Login user
POST   /api/auth/verify-email          Verify email code
POST   /api/auth/resend-verification-email  Resend code
POST   /api/auth/refresh               Refresh access token
POST   /api/auth/forgot-password       Request password reset
POST   /api/auth/verify-reset-code     Verify reset code
POST   /api/auth/reset-password        Update password
POST   /api/auth/resend-reset-code     Resend reset code
DELETE /api/auth/account               Delete account
```

### Note Endpoints (Authenticated)
```
GET    /api/notes                      Get all notes
POST   /api/notes                      Create note
PUT    /api/notes/{id}                 Update note
DELETE /api/notes/{id}                 Delete note
```

### Quiz Endpoints (Authenticated)
```
POST   /api/quiz-results               Submit quiz result
GET    /api/quiz-results               Get all results
GET    /api/quiz-results/domain/{domain}      Get domain results
GET    /api/quiz-results/domain/{domain}/best Get best score
GET    /api/quiz-results/statistics    Get statistics
```

### Health Endpoints
```
GET    /health                         Health check
GET    /ping                           Ping
```

---

## 16. COMMON WORKFLOWS

### Workflow: New User Registration to Login

1. **Register:** POST /api/auth/register
   - User provides email, password, name
   - Backend creates user, sends verification email
   - Returns user info (no tokens yet)

2. **Verify Email:** POST /api/auth/verify-email
   - User enters 6-digit code from email
   - Backend validates code, marks verified
   - Returns access + refresh tokens

3. **Make Authenticated Requests:**
   - Frontend includes: `Authorization: Bearer {accessToken}`
   - JwtAuthenticationFilter validates token
   - Request proceeds to controller

4. **Token Refresh:** POST /api/auth/refresh
   - When access token expires (24h)
   - Frontend sends refresh token
   - Backend returns new access token

### Workflow: Password Reset

1. **Request Reset:** POST /api/auth/forgot-password
   - User provides email
   - Backend generates code, sends email
   - Returns immediately

2. **Verify Code:** POST /api/auth/verify-reset-code
   - User enters 6-digit code
   - Backend validates code
   - Returns validation result

3. **Reset Password:** POST /api/auth/reset-password
   - User provides code + new password
   - Backend updates password, marks token used
   - Returns success

### Workflow: Authenticated Request

1. Frontend has access token from login
2. Request includes: `Authorization: Bearer {token}`
3. JwtAuthenticationFilter intercepts
4. Validates JWT signature + expiration
5. Loads user from database
6. Sets authentication in SecurityContext
7. Request proceeds to controller
8. Controller accesses current user via SecurityContext

---

## 17. SECURITY BEST PRACTICES IMPLEMENTED

1. **Password Hashing** - BCrypt with 10 rounds
2. **JWT Tokens** - Signed with HMAC-SHA256
3. **Token Expiration** - Access (24h), Refresh (7d)
4. **Stateless Auth** - No sessions, scale-friendly
5. **CSRF Protection** - Disabled (safe for JWT)
6. **CORS** - Restricted to known domains
7. **Email Verification** - Prevents fake emails
8. **Code Expiration** - 15 minutes for verification/reset
9. **Code Reuse Prevention** - Tokens marked as used
10. **Cascade Delete** - No orphaned data
11. **Password Validation** - Min 8 characters
12. **Async Processing** - No email delays
13. **Multi-App Isolation** - Separate accounts per app

---

## 18. ERROR HANDLING

### Common Exceptions

```java
RuntimeException("Email already registered")
RuntimeException("User not found")
RuntimeException("Email not verified")
RuntimeException("Invalid verification code")
RuntimeException("Verification code expired")
RuntimeException("Invalid or expired refresh token")

IllegalArgumentException("No account found with this email")
IllegalArgumentException("Invalid password")
IllegalArgumentException("Invalid or expired reset code")
```

### HTTP Status Codes

- **200 OK** - Success
- **400 Bad Request** - Validation error
- **401 Unauthorized** - Invalid credentials, expired token
- **403 Forbidden** - Access denied
- **404 Not Found** - Resource not found
- **500 Internal Server Error** - Server error

---

## 19. DEVELOPMENT TOOLS & TESTING

### Local Development Setup

```bash
# Prerequisites
- Java 21
- PostgreSQL
- Maven

# Environment (.env file)
DB_USERNAME=postgres
DB_PASSWORD=...
JWT_SECRET=base64-encoded-key
RESEND_API_KEY=...
DEV_MODE=true
```

### Testing with Postman

- Create collection for all endpoints
- Use environment variables for base URL
- Pre-request scripts for token management
- Tests for success and error cases

---

## 20. KEY LEARNINGS & PATTERNS

### Design Patterns Used

1. **MVC** - Model-View-Controller (REST variant)
2. **Repository** - Data access abstraction
3. **Dependency Injection** - Spring's @Autowired
4. **Builder** - Lombok @Builder for entities
5. **Factory** - Spring creates beans
6. **Strategy** - Multiple email providers
7. **Async/Callback** - @Async for background tasks

### Best Practices

1. **DTO Pattern** - Separate entity from API contract
2. **Service Layer** - Business logic separate from HTTP
3. **Validation** - @Valid, @NotBlank
4. **Logging** - SLF4J with @Slf4j
5. **Transactions** - @Transactional for consistency
6. **Security** - Authentication, authorization, encryption
7. **Documentation** - Code comments, README

---

## 21. COMMON DEBUGGING TECHNIQUES

### Enable Debug Logging

```yaml
logging:
  level:
    com.authservice: DEBUG
    org.springframework.security: DEBUG
```

### Check JWT Tokens (Frontend)

```javascript
const token = "eyJhbGc...";
const parts = token.split('.');
const decoded = JSON.parse(atob(parts[1]));
console.log(decoded);
```

### Database Queries

```yaml
spring.jpa.show-sql: true
spring.jpa.properties.hibernate.format_sql: true
```

---

## 22. DEPLOYMENT CHECKLIST

### Environment Variables Needed

```
DB_USERNAME
DB_PASSWORD
JWT_SECRET (Base64 encoded 256-bit key)
RESEND_API_KEY
RESEND_FROM_EMAIL
AWS_SES_SMTP_USERNAME
AWS_SES_SMTP_PASSWORD
DATABASE_URL (Production)
SPRING_PROFILES_ACTIVE=prod
PORT
```

### Production Checklist

- [ ] Set `app.dev-mode=false`
- [ ] Set logging to WARN level
- [ ] Configure production database URL
- [ ] Set up email providers
- [ ] Implement rate limiting
- [ ] Add monitoring/alerting
- [ ] Set up CI/CD pipeline
- [ ] Use HTTPS only
- [ ] Implement CORS whitelist
- [ ] Test password reset flow
- [ ] Load test API
- [ ] Set up database backups

---

## 23. QUICK REFERENCE: KEY FILES TO UNDERSTAND

### Start Here

1. `AuthMicroserviceApplication.java` - Entry point
2. `SecurityConfig.java` - Security setup
3. `AuthService.java` - Core auth logic
4. `User.java` - User entity

### Then Study

5. `AuthController.java` - REST endpoints
6. `JWTService.java` - Token handling
7. `ResendEmailService.java` - Email logic
8. `UserRepository.java` - Database queries

### Deep Dive

9. `JwtAuthenticationFilter.java` - Request validation
10. `PasswordResetService.java` - Password reset flow
11. `NoteService.java` - Note CRUD
12. `QuizResultService.java` - Quiz tracking

---

## SUMMARY

This authentication microservice is a production-ready authentication system that demonstrates:

- Secure authentication with JWT and BCrypt
- Email verification with time-limited codes
- Password reset with secure code-based flow
- Multi-app support with data isolation
- Async processing for optimal performance
- Professional email templates
- Additional features (notes, quiz tracking)
- Clean architecture with separation of concerns
- Security best practices throughout

It's a comprehensive learning resource and production-ready component for any web application needing authentication.

---

## STUDY TIPS

1. **Start with the flow** - Understand registration → verification → login
2. **Read the controllers** - See how HTTP requests are handled
3. **Study the services** - Understand business logic
4. **Examine the models** - Learn database structure
5. **Debug locally** - Set breakpoints, watch variables
6. **Test with Postman** - Make real API calls
7. **Read error messages** - Understand what went wrong
8. **Check logs** - See what's happening under the hood
9. **Experiment** - Modify code, see what breaks
10. **Build something new** - Add a feature to practice

Good luck with your studies!
