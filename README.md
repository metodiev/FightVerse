# FightVerse

[![Backend CI](https://github.com/metodiev/FightVerse/actions/workflows/backend-ci.yml/badge.svg?branch=main)](https://github.com/metodiev/FightVerse/actions/workflows/backend-ci.yml)

Modular-monolith backend for the FightVerse martial-arts platform.

## Layout

| Path | Contents |
| ---- | -------- |
| `backend/` | Spring Boot 4.1 application (Java 25) |
| `docs/api/` | `openapi.yaml` — the HTTP contract, validated by the test suite |
| `ai/`, `web/`, `mobile/`, `vr/`, `infra/` | Not implemented yet |

## Toolchain

The build fails fast on the wrong toolchain via `maven-enforcer-plugin`:

- **JDK 25** (LTS). There is no default `java` on `PATH` on the reference machine, so set it explicitly.
- **Maven 3.9+**. No Maven Wrapper is committed.

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home
cd backend
mvn clean test        # compile + unit, contract and integration tests
mvn spring-boot:run   # needs PostgreSQL, see below
```

## Running locally

The service needs PostgreSQL. Flyway owns the schema (`ddl-auto: validate` means Hibernate never
creates tables), so migrations run automatically at startup.

```bash
docker run --rm -p 5432:5432 -e POSTGRES_DB=fightverse \
  -e POSTGRES_USER=fightverse -e POSTGRES_PASSWORD=fightverse postgres:17-alpine
```

Configuration is environment-driven — see `backend/src/main/resources/application.yml`:
`FIGHTVERSE_DB_URL`, `FIGHTVERSE_DB_USER`, `FIGHTVERSE_DB_PASSWORD`, `FIGHTVERSE_JWT_SECRET`,
`FIGHTVERSE_CORS_ALLOWED_ORIGINS`. The default JWT secret is a development placeholder and the
`prod` profile refuses to start while it is in use.

## Tests

| Suite | What it covers |
| ----- | -------------- |
| `**/*Test` | Unit tests for services, domain rules, error mapping and security plumbing |
| `contract/OpenApiContractTest` | Validates real MVC responses against `docs/api/openapi.yaml` |
| `integration/PostgresSmokeTest` | Flyway + Hibernate + BCrypt + JWT against a real PostgreSQL container |

`PostgresSmokeTest` requires Docker and **skips** (rather than fails) when no daemon is available, so a
green local run without Docker does not mean the database path has been verified. CI runs on
`ubuntu-latest`, which has a Docker daemon, so there the suite executes against a real PostgreSQL 17.

## API

The contract lives in [`docs/api/openapi.yaml`](docs/api/openapi.yaml). The build copies it onto the test
classpath so `OpenApiContractTest` can assert that controllers still match it.

## Content

Schema and reference data are seeded by migration, not code (`V1`-`V8`):

- 8 disciplines (`V6__seed_disciplines.sql`)
- 3 boxing techniques — jab, cross, guard recovery (`V7__seed_boxing_techniques.sql`)

Only boxing has techniques so far. The seeded evaluation criteria are a prototype and are marked in
`V7` as needing validation by a professional instructor before they are used for scoring.
