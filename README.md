# Expense Tracker - Backend

Spring Boot 3.3 / Java 17 REST API with MySQL, Spring Security and JWT.

```powershell
cd "C:\path\to\expense-tracker\backend"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_mysql_password"
mvn spring-boot:run
```

```powershell
cd "C:\path\to\expense-tracker\backend"
mvn test
```

Runs on http://localhost:8080. Configuration lives in `src/main/resources/application.properties`
(`DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION_MS`, `CORS_ALLOWED_ORIGINS`).
See the root README for the full API reference.

Packages: `controller` (REST endpoints), `service` (business logic), `repository` (Spring Data JPA),
`entity` (User, Transaction, Budget), `dto` (request/response records), `exception` (custom exceptions and
global handler), `config` (security, JWT).
