@echo off
REM Security Test Runner Script for RentPro Backend (Windows)
REM This script runs comprehensive security tests

echo 🔒 Running Security Tests for RentPro Backend
echo ==========================================

REM Set test profile
set SPRING_PROFILES_ACTIVE=test

echo.
echo 📋 Running Input Sanitization Tests...
call mvn test -Dtest=InputSanitizerTest -q

echo.
echo 🛡️ Running Security Integration Tests...
call mvn test -Dtest=SecurityIntegrationTests -q

echo.
echo 🔍 Running All Security Tests...
call mvn test -Dtest="**/*Security*Test,**/*Sanitiz*Test" -q

echo.
echo 📊 Running Security Vulnerability Scan...
REM Check for known vulnerabilities in dependencies
call mvn dependency-check:check -q
if errorlevel 1 echo ⚠️  Dependency check failed - review vulnerabilities

echo.
echo ✅ Security Tests Complete!
echo.
echo 📋 Test Summary:
echo   - Input Sanitization: Tests XSS and SQL injection prevention
echo   - Security Integration: Tests rate limiting, validation, and headers
echo   - Dependency Check: Scans for known vulnerabilities
echo.
echo 🔧 To run individual test categories:
echo   mvn test -Dtest=InputSanitizerTest
echo   mvn test -Dtest=SecurityIntegrationTests
echo   mvn dependency-check:check
echo.
echo 🌐 To test with specific profiles:
echo   SPRING_PROFILES_ACTIVE=test mvn test -Dtest=SecurityIntegrationTests

pause
