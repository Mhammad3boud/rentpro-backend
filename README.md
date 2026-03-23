# RentPro Backend

Spring Boot backend API for RentPro property management.

## Tech Stack
- Java 17
- Spring Boot 4
- Spring Security (JWT)
- Spring Data JPA
- PostgreSQL
- Flyway migrations
- Maven

## Core Modules
- `auth` (login/register, JWT issuance)
- `user`
- `property`
- `unit`
- `tenant`
- `lease`
- `payment`
- `maintenance`
- `contract`
- `dashboard`
- `notification`
- `activity`
- `ai` (prediction endpoints)
- `config` / `security` / `filter`

## Security Highlights
- Stateless JWT authentication
- Role-based access control (OWNER / TENANT)
- Rate limiting filter
- Configurable security headers
- CORS configured for local Ionic/Capacitor clients

## Prerequisites
- Java 17+
- Maven 3.9+ (or use `mvnw.cmd`)
- PostgreSQL 14+

## Configuration
Main app config is in:
- `src/main/resources/application.properties`

Default server port:
- `8083`

### Environment Variables
Use `.env.example` as template and export values in your environment:
- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `JWT_SECRET`
- `JWT_TTL`
- `RATE_LIMIT_AUTH`
- `RATE_LIMIT_API`
- `RATE_LIMIT_UPLOAD`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `STORAGE_PROVIDER` (`local` or `s3`)
- `S3_BUCKET`, `S3_REGION`, `S3_ACCESS_KEY`, `S3_SECRET_KEY` (when using `s3`)
- Optional S3-compatible keys: `S3_ENDPOINT`, `S3_PUBLIC_BASE_URL`, `S3_KEY_PREFIX`, `S3_PATH_STYLE_ACCESS`
- Optional app version keys (`APP_VERSION_*`)

> Note: Spring Boot reads OS environment variables by default. If you use a `.env` file, load it via your shell/IDE before starting the app.

### File Storage (Production Recommendation)
- Use `STORAGE_PROVIDER=s3` in production.
- Uploaded files are stored in object storage, while only URLs are stored in PostgreSQL.
- Keep `STORAGE_PROVIDER=local` for local development convenience.

## Database & Migrations
Flyway migrations are in:
- `src/main/resources/db/migration`

On startup, migrations run automatically when Flyway is enabled.

## Run (Development)
Using Maven wrapper on Windows:
```bash
.\mvnw.cmd spring-boot:run
```

Using Maven:
```bash
mvn spring-boot:run
```

API base URL:
- `http://localhost:8083`

## Build
```bash
.\mvnw.cmd clean package
```

## Test
```bash
.\mvnw.cmd test
```

## Important Endpoints (High Level)
- `/auth/**` public authentication routes
- `/api/app-config/**` public app-version/config routes
- `/api/properties/**` OWNER role
- `/api/tenants/**` OWNER or TENANT role
- `/api/leases/**` OWNER or TENANT role
- `/payments/**` OWNER or TENANT role
- `/dashboard/**` OWNER or TENANT role
- `/maintenance/**` OWNER or TENANT role

## Project Structure
```text
src/main/java/com/rentpro/backend/
  auth/ property/ tenant/ lease/ payment/ maintenance/ ...
src/main/resources/
  application.properties
  db/migration/
```

## Security and Hardening Checklist
- Replace default JWT secret in production
- Use strong DB credentials and secret management
- Restrict CORS origins for production domains
- Disable verbose SQL/security logs in production
- Configure HTTPS and HSTS in deployment
