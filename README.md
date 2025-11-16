# Auth Microservice

A Spring Boot authentication and authorization microservice which will include **JWT** tokens, **email verification**, and **SMS verification** capabilities.

## Project Status

### Implemented 
- **User Registration** - Creates new user account with email and password
- **Email Verification** - 6-digit verification code with a 15 minute expiration time
- **User Login** - JWT-based authentication with access and refresh tokens
- **Email Notifications** - Added HTML email templates for verification and welcome messages 
- **PostgresSQL Integration** - Persistent data storage with JPA/Hibernate
- **Spring Security** - Comprehensive security configuration

**to do's:** 
- Token refresh endpoint
- Improving Documentation
- Twilio integration
- Role-based access control (RBAC)
- OAuth2 integration 
- Rate limiting
- Docker containerization
- Comprehensive testing


## Overview 

A microservice which can process user authentication and authorization for modern applications, offering a secure and scalable solution that can be easily integrated into my frontend projects.

### Key Features

- **JWT Authentication**: Secure token-based authentication system
- **Email Verification**: User email validation during registration


## Technology Stack

- **Framework**: Spring Boot
- **Security**: Spring Security with JWT
- **SMS Provider**: Twilio
- **Build tool**: Maven
- **Java Version**: 21


### Why am I building this?
This authentication microservice was built as a functional and educational self-starter project to better understand backend development and the microservice architecture. 


## Acknowledgments

- Built with help from Claude AI (Anthropic) for starter files and some boiler-plate code which helped me get the project started quicker; I'm also trying to implement the 70-30 rule, or in other words, trying to keep ai generated code limited to 20-30%
- Twilio for SMS verification


## About This Project

- JWT-based authentication
- Email/SMS verification flows
- Spring Security architecture
- Microservice design patterns



