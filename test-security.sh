#!/bin/bash

# Security Test Runner Script for RentPro Backend
# This script runs comprehensive security tests

echo "🔒 Running Security Tests for RentPro Backend"
echo "=========================================="

# Set test profile
export SPRING_PROFILES_ACTIVE=test

# Run security-specific tests
echo "📋 Running Input Sanitization Tests..."
mvn test -Dtest=InputSanitizerTest -q

echo ""
echo "🛡️ Running Security Integration Tests..."
mvn test -Dtest=SecurityIntegrationTests -q

echo ""
echo "🔍 Running All Security Tests..."
mvn test -Dtest="**/*Security*Test,**/*Sanitiz*Test" -q

echo ""
echo "📊 Running Security Vulnerability Scan..."
# Check for known vulnerabilities in dependencies
mvn dependency-check:check -q || echo "⚠️  Dependency check failed - review vulnerabilities"

echo ""
echo "✅ Security Tests Complete!"
echo ""
echo "📋 Test Summary:"
echo "  - Input Sanitization: Tests XSS and SQL injection prevention"
echo "  - Security Integration: Tests rate limiting, validation, and headers"
echo "  - Dependency Check: Scans for known vulnerabilities"
echo ""
echo "🔧 To run individual test categories:"
echo "  mvn test -Dtest=InputSanitizerTest"
echo "  mvn test -Dtest=SecurityIntegrationTests"
echo "  mvn dependency-check:check"
echo ""
echo "🌐 To test with specific profiles:"
echo "  SPRING_PROFILES_ACTIVE=test mvn test -Dtest=SecurityIntegrationTests"
