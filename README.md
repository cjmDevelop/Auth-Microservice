# Auth Microservice with Password Reset

A Spring Boot authentication and authorization microservice featuring **JWT tokens**, **email verification**, and **password reset** capabilities - built as a learning project and portfolio piece.

##  Project Status

### ✅ Implemented Features
- **User Registration** - Secure account creation with email and password
- **Email Verification** - 6-digit verification codes (15-minute expiration)
- **User Login** - JWT-based authentication with access and refresh tokens
- **Password Reset** - Secure forgot password flow with time-limited codes
- **Email Notifications** - Beautiful HTML email templates for all user interactions
- **PostgreSQL Integration** - Persistent data storage with JPA/Hibernate
- **Spring Security** - Comprehensive security configuration with BCrypt password encryption

### 🔜 Planned Enhancements
- Token refresh endpoint
- SMS verification via Twilio
- Role-based access control (RBAC)
- OAuth2 integration (Google, GitHub)
- Rate limiting and brute force protection
- Docker containerization
- Comprehensive unit and integration testing
- API documentation (Swagger/OpenAPI)

## 📖 Overview

A microservice designed to handle user authentication and authorization for modern web applications. Built with security best practices and scalability in mind, this service provides everything needed for user account management and can be easily integrated into any frontend project.

###  Key Features

- **JWT Authentication**: Secure, stateless token-based authentication
- **Email Verification**: User email validation with time-limited codes
- **Password Reset**: Complete forgot password flow with email verification
- **Security**: BCrypt password hashing, token expiration, CORS configuration
- **Email System**: Professional HTML email templates with async sending
- **RESTful API**: Clean, well-documented endpoints

## Tech Stack

- **Framework**: Spring Boot 3.x
- **Language**: Java 21
- **Security**: Spring Security with JWT
- **Database**: PostgreSQL
- **Email**: Spring Mail (SMTP)
- **SMS**: Twilio (planned)
- **Build Tool**: Maven
- **ORM**: Hibernate/JPA



##  Why I Built This

This authentication microservice started as a functional learning project to better understand:
- Backend development with Spring Boot
- Microservice architecture patterns
- Security best practices (JWT, encryption, token management)
- Database design and relationships
- Email integration and async processing
- RESTful API design

It has evolved into a production-ready component that I'm proud to showcase in my portfolio and use across my frontend projects like [Random Writes Random Lights](https://randomwritesrandomlights.com).

##  Development Approach

I built this project following professional development practices:
- **Test-Driven Mindset**: Testing each feature thoroughly with Postman
- **Security First**: Implementing encryption, validation, and expiration from the start
- **Clean Code**: Following Spring Boot conventions and best practices
- **Documentation**: Comprehensive comments and clear API contracts

### Development with AI

This project was developed with **Claude AI (Anthropic)** as a collaborative coding partner. Claude helped me:
- Understand Spring Boot architecture and best practices
- Debug complex issues and explain the why behind solutions
- Structure the codebase professionally
- Learn security concepts and implementation patterns


## What I Learned

Building this microservice taught me:
- **Spring Boot ecosystem**: Security, JPA, Mail, Validation
- **Database relationships**: Foreign keys, cascading, transactions
- **Security patterns**: JWT, password hashing, token expiration
- **Email integration**: Async processing, HTML templates, SMTP
- **API design**: RESTful principles, error handling, validation
- **Deployment**: Environment configuration, secrets management
- **Problem-solving**: Debugging, reading documentation, asking good questions

## Acknowledgments

- **Claude AI (Anthropic)**: For being an exceptional coding mentor, pair programmer, and patient teacher. The collaborative development process was invaluable for learning.
- **Spring Boot Community**: For excellent documentation and examples


