# Transaction reconciliation

A Spring Boot service that reconciles a financial institution's internal ledger with an external payment processor. It accepts transaction records from both sources, validates and normalises them, stores them in PostgreSQL, and raises discrepancies for investigation.

Work in progress.

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

The local database name, user, and password are `reconciliation`. Those values are the defaults in `compose.yaml`. Copy `.env.example` to `.env` to override them.

From the project directory:

```powershell
docker compose up -d
docker compose ps
```

`docker compose ps` should show all three services as healthy.

`docker compose down` stops the containers and keeps their data. `docker compose down -v` deletes that data.

## Run the application

```powershell
.\mvnw.cmd spring-boot:run
```

The application listens on port 8080. It connects to PostgreSQL at `localhost:5432`, database `reconciliation`, with the local user and password `reconciliation`. It connects to Redis at `localhost:6379`.

`reconciliation` is a local development password for the empty database created by Docker. `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` override those settings when they are present in the process environment.

On startup, Flyway applies any pending migrations. Redis needs to be running before a submission will succeed.

Run the tests with the same containers already up:

```powershell
.\mvnw.cmd test
```

## What works now

- Create and retrieve institutions.
- Register an integration source for an institution. A source is an internal ledger or a payment processor, and its name is unique within that institution.
- Accept a transaction with `POST /api/v1/sources/{id}/transactions`, which returns `202 Accepted`. Amounts are integer minor units, so £250.00 is sent as `25000`. Retrieve one transaction, or a filtered page of them.
- Normalise provider words before the row is stored. `SALE` is stored as `PAYMENT`, `gbp` as `GBP`, and `settled` is kept as the provider's own word and stored as normalised status `COMPLETED`. The platform's processing status starts as `PENDING`.
- Require an `Idempotency-Key` header on submission to implement idempotency with Redis, so a repeated submission returns the original transaction.
- Publish each accepted transaction to Kafka. The consumer loads that transaction and moves its processing status from `PENDING` to `PROCESSED`.
- Retry processing that fails. When the attempts are used up, the transaction is stored as `FAILED` and its id is published to a dead-letter topic.

## Still to come

- Compare an institution's internal ledger with its payment processor over a period, and store the outcome of each comparison.
- Open an exception for a discrepancy, and keep a history of status changes.
- Authenticate each integration source with an API key.
- Run the tests in GitHub Actions.
