# Local Development

## Requirements

- Java 21
- Docker Desktop
- IntelliJ IDEA
- PostgreSQL via Docker Compose
- Gradle Wrapper

## Backend

Backend location:

    backend/

Use Gradle Wrapper from the backend folder:

    cd backend
    .\gradlew.bat clean build
    .\gradlew.bat :barter-web:bootRun

Flyway PostgreSQL support for Flyway 11 is provided by the runtime dependency `org.flywaydb:flyway-database-postgresql` in `barter-web`.

## Database

Local database will use PostgreSQL through Docker Compose.

Start the database:

    docker compose up -d

Check status:

    docker compose ps

Stop the database:

    docker compose down

Stop and remove the data volume:

    docker compose down -v

Default local database:

    database: barter_db
    username: barter_user
    password: barter_password
    port: 5432

## Profiles

Local Spring profile:

    local

Application config:

    barter-web/src/main/resources/application.yml
    barter-web/src/main/resources/application-local.yml

## Rule

Do not commit local environment secrets.

Use .env files for local-only configuration when needed.