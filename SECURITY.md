# Security Implementation Guide

This document outlines the security measures implemented in the RentPro backend application following OWASP best practices.

## 🛡️ Security Features Implemented

### 1. Rate Limiting
- **Authentication endpoints**: 5 requests per minute per IP/user
- **General API endpoints**: 100 requests per minute per IP/user  
- **File upload endpoints**: 10 requests per minute per IP/user
- **Features**:
  - IP-based and user-based rate limiting
  - Graceful 429 responses with Retry-After headers
  - Distributed bucket storage for scalability
  - Automatic bucket cleanup

### 2. Input Validation & Sanitization
- **Schema-based validation** using Jakarta Bean Validation annotations
- **Type checking** with strict constraints
- **Length limits** on all user inputs
- **XSS prevention** through HTML escaping and pattern filtering
- **SQL injection prevention** through pattern detection
- **Field rejection** for unexpected parameters

### 3. Secure API Key Handling
- **Environment variables** for all sensitive configuration
- **No hardcoded secrets** in source code
- **JWT secret rotation** support
- **Database credentials** externalized
- **Email credentials** externalized

### 4. Security Headers
- **XSS Protection**: `1; mode=block`
- **Content-Type Options**: `nosniff`
- **Frame Options**: `DENY` (prevents clickjacking)
- **Strict Transport Security**: HTTPS enforcement
- **Content Security Policy**: Restricts resource loading
- **Referrer Policy**: Controls referrer information

## 🔧 Configuration

### Environment Setup
1. Copy `.env.example` to `.env`
2. Update with your actual values
3. Ensure `.env` is in `.gitignore`

### Production Configuration
```bash
export JWT_SECRET="your_32_character_minimum_random_secret"
export DATABASE_PASSWORD="your_secure_db_password"
export MAIL_PASSWORD="your_app_specific_password"
export SECURITY_LOG_LEVEL="WARN"
export ERROR_INCLUDE_MESSAGE="never"
```

## 📝 Security Guidelines

### Password Requirements
- Minimum 8 characters
- Maximum 128 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one digit
- At least one special character (@$!%*?&)

### Input Validation Rules
- **Email**: RFC 5322 compliant, max 254 characters
- **Phone**: E.164 format, international support
- **Names**: Letters, spaces, hyphens, apostrophes only
- **Search queries**: Max 100 characters, sanitized

### Rate Limiting Strategy
- **Authentication**: Strict limits to prevent brute force
- **API**: Balanced limits for legitimate use
- **Uploads**: Conservative limits to prevent abuse

## 🚀 Deployment Security

### Pre-deployment Checklist
- [ ] Set strong JWT secret (32+ chars)
- [ ] Configure environment variables
- [ ] Enable HTTPS
- [ ] Set production logging levels
- [ ] Disable error details in responses
- [ ] Configure database firewall
- [ ] Set up monitoring and alerts

### Docker Security
```dockerfile
# Use non-root user
USER appuser

# Read-only filesystem where possible
VOLUME ["/tmp"]

# Minimal base image
FROM eclipse-temurin:17-jre-alpine
```

## 🔍 Security Testing

### Automated Tests
```bash
# Run security tests
mvn test -Dtest=SecurityTests

# Check for vulnerabilities
mvn dependency-check:check
```

### Manual Testing
1. **Rate Limiting**: Use tools like JMeter or curl with timing
2. **Input Validation**: Test with malicious payloads
3. **Authentication**: Test JWT token manipulation
4. **Headers**: Verify security headers in responses

## 📊 Monitoring

### Security Metrics
- Rate limit violations
- Authentication failures
- Input validation errors
- Unusual request patterns
- Security header compliance

### Alerting
- High rate of 429 responses
- Brute force attack detection
- SQL injection attempt patterns
- XSS attempt patterns

## 🔄 Security Maintenance

### Regular Tasks
- **Monthly**: Review and rotate secrets
- **Quarterly**: Update dependencies
- **Annually**: Security audit and penetration testing

### Incident Response
1. **Detection**: Monitor logs and metrics
2. **Containment**: Block malicious IPs
3. **Investigation**: Analyze attack vectors
4. **Recovery**: Patch vulnerabilities
5. **Post-mortem**: Document and improve

## 🛠️ Development Security

### Secure Coding Practices
- Never commit secrets to version control
- Use parameterized queries
- Validate all inputs
- Implement least privilege access
- Regular security training

### Code Review Checklist
- [ ] Input validation implemented
- [ ] Error handling doesn't leak information
- [ ] Authentication/authorization checks
- [ ] SQL injection prevention
- [ ] XSS prevention
- [ ] Rate limiting applied

## 📚 References

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [OWASP Security Headers](https://owasp.org/www-project-secure-headers/)
- [OWASP Rate Limiting](https://cheatsheetseries.owasp.org/cheatsheets/Rate_Limiting_Cheat_Sheet.html)
- [OWASP Input Validation](https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html)

## 🆘 Support

For security issues:
1. Do NOT open public issues
2. Email: security@rentpro.com
3. Include detailed vulnerability information
4. Allow reasonable time for response before disclosure

---

**Remember**: Security is an ongoing process, not a one-time implementation. Regular updates and monitoring are essential for maintaining security posture.
