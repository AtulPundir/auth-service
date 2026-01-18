# Setup Instructions

## Quick Start

### 1. Create Database

```bash
# Using psql (Note: PostgreSQL doesn't like hyphens in database names, so we use auth_db)
/usr/local/Cellar/postgresql@15/15.15_1/bin/psql -U atulpundir -d postgres -c 'CREATE DATABASE "auth-db";'

# Or if auth-db already exists, you can drop and recreate
/usr/local/Cellar/postgresql@15/15.15_1/bin/psql -U atulpundir -d postgres -c 'DROP DATABASE IF EXISTS "auth-db";'
/usr/local/Cellar/postgresql@15/15.15_1/bin/psql -U atulpundir -d postgres -c 'CREATE DATABASE "auth-db";'
```

### 2. Run Flyway Migrations

The migrations will run automatically when you start the application thanks to Spring Boot's Flyway integration.

But you can also run them manually using Gradle:

```bash
cd /Users/atulpundir/Projects/MYPROJECT/myAppJava/auth-service
./gradlew flywayMigrate
```

### 3. Build the Project

```bash
./gradlew clean build
```

### 4. Run the Application

```bash
./gradlew bootRun --args='--enable-preview'
```

Or just:

```bash
./gradlew bootRun
```

The application will start on http://localhost:3000

## Verify Migrations

Check if tables were created:

```bash
/usr/local/Cellar/postgresql@15/15.15_1/bin/psql -U atulpundir -d "auth-db" -c "\dt"
```

Expected output:
```
                   List of relations
 Schema |           Name           | Type  |   Owner
--------+--------------------------+-------+------------
 public | audit_logs               | table | atulpundir
 public | flyway_schema_history    | table | atulpundir
 public | otp_codes                | table | atulpundir
 public | refresh_tokens           | table | atulpundir
 public | users                    | table | atulpundir
```

## Troubleshooting

### Issue: "schema-validation: missing column"

**Solution**: Change `ddl-auto: validate` to `ddl-auto: none` in application.yml (already done)

### Issue: Flyway migration failed

**Solution**: Drop database and recreate:
```bash
/usr/local/Cellar/postgresql@15/15.15_1/bin/psql -U atulpundir -d postgres -c 'DROP DATABASE "auth-db" CASCADE;'
/usr/local/Cellar/postgresql@15/15.15_1/bin/psql -U atulpundir -d postgres -c 'CREATE DATABASE "auth-db";'
./gradlew flywayMigrate
```

### Issue: Redis connection failed

**Solution**: Make sure Redis is running:
```bash
# Check if Redis is running
redis-cli ping

# Should return: PONG
```

If Redis is not running, you can disable it temporarily by commenting out the Redis configuration in application.yml.

## Next Steps

Once the application starts successfully:

1. Check health endpoint: `curl http://localhost:3000/actuator/health`
2. View Swagger UI: http://localhost:3000/swagger-ui.html (once implemented)
3. Check logs: `tail -f logs/auth-service.log`
