# Transaction reconciliation

Spring Boot service for comparing a bank's internal transaction ledger with records from an external payment processor.

## Requirements

- JDK 21
- Docker Desktop, using Linux containers

## Local infrastructure

PostgreSQL, Kafka, and Redis run in Docker. The application runs on the host through Maven.

| Service    | Image               | Host port |
|------------|---------------------|-----------|
| PostgreSQL | `postgres:18`       | 5432      |
| Kafka      | `apache/kafka:4.4.0`| 9092      |
| Redis      | `redis:8.10.3`      | 6379      |

The local database name, user, and password are `reconciliation`. Those values are the defaults in `compose.yaml`. Create an `.env` file, and copy `.env.example` to `.env` to override them.

From the project directory:

```powershell
docker compose up -d
docker compose ps
```

`docker compose ps` should show all three services as healthy.

`docker compose down` stops the containers and keeps the database volume. `docker compose down -v` deletes that volume and the data in it.

## Run the application

```powershell
.\mvnw.cmd spring-boot:run
```

The application listens on port 8080.

## Roadmap

- Connect the application to PostgreSQL, Kafka, and Redis.
- Accept transaction records and store them.
- Reconcile the internal ledger against the payment processor.
