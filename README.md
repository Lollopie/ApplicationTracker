# Job Application Tracker API

A REST API for tracking job applications through their lifecycle, from saved to applied, interview, offer or rejected. Each user sees only their own applications. Every status change is recorded in a history, and the allowed transitions are enforced by the domain model.

Built as a learning and portfolio project, with a focus on doing the backend fundamentals properly: authentication, data isolation, concurrency, testing and delivery.

## Tech stack

- **Java 21**, **Spring Boot 4.1** (Web MVC, Data JPA, Validation, Security 7, Actuator)
- **PostgreSQL 18** with **Flyway** migrations, Hibernate 7
- **Testcontainers**, JUnit 5, MockMvc, `RestTestClient`
- **Docker** (layered, non-root image) and **GitHub Actions** CI publishing to GitHub Container Registry

## Features

- **Session-based authentication:** JSON login, server-side sessions (`JSESSIONID`), session fixation protection, and BCrypt behind a delegating password encoder. The password hash is erased from the session's principal after login.
- **CSRF protection:** double-submit cookie (`XSRF-TOKEN` cookie plus `X-XSRF-TOKEN` header), with the token rotated on login.
- **Per-user data isolation:** every query is scoped to the logged-in user. Another user's application returns `404`, not `403`, so the API doesn't reveal that it exists.
- **Status workflow:** an enum-based state machine with an append-only status history.
- **Optimistic locking:** concurrent modifications return `409 Conflict` instead of silently overwriting each other.
- **Filtering, sorting and pagination:** an allowlist of sortable fields, a capped page size, and rejection of unknown query parameters.
- **Consistent errors:** every error is an [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457) response.

## Getting started

### Prerequisites

- JDK 21
- Docker (for the database, the integration tests and the container image)

### Run with Docker Compose

```bash
cp .env.example .env        # then fill in your own values
./mvnw clean package        # the image is built from the tested jar in target/
docker compose up --build
```

The API is then available at `http://localhost:8080`. You can check it with `GET /actuator/health`.

The admin password hash in `.env` must be a BCrypt hash prefixed with `{bcrypt}`, for example:

```bash
htpasswd -bnBC 12 "" 'your-admin-password' | tr -d ':\n'
```

Keep the value in **single quotes** in `.env`, so Compose doesn't try to expand the `$` characters in the hash.

### Run from the IDE

Provide the same variables (`DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `ADMIN_PASSWORD_HASH`) as environment variables in your run configuration. Point `DATABASE_URL` at `localhost`, for example `jdbc:postgresql://localhost:5432/jobtracker`, and start only the database with `docker compose up postgres`.

### Tests

```bash
./mvnw test     # unit and web-slice tests, no Docker needed
./mvnw verify   # also runs the integration tests (*IT) against PostgreSQL 18 via Testcontainers
```

## API overview

All request and response bodies are JSON. Requests that change state (`POST`, `PUT`, `DELETE`) need a CSRF token.

### Authentication flow

1. `GET /csrf` sets the `XSRF-TOKEN` cookie.
2. `POST /register` with `{ "username": "...", "password": "..." }` returns `201`, or `409` if the username is taken. Usernames are 1 to 32 characters (letters, digits, `.`, `_` and `-`), and passwords are 8 to 64 characters.
3. `POST /login` with the same body returns `200` and a `JSESSIONID` cookie. Login only checks that both fields are present, so tightening the registration rules later can't lock out existing users.
4. The CSRF token changes on login, so call `GET /csrf` again before your next state-changing request.
5. `POST /logout` ends the session.

Send the cookies back with every request, and send the `XSRF-TOKEN` cookie's value in the `X-XSRF-TOKEN` header.

### Applications

| Method | Path | Description |
|---|---|---|
| `POST` | `/applications` | Create an application with `company`, `position`, an initial `status`, and optional `notes` and `links`. Returns `201` with a `Location` header. |
| `GET` | `/applications` | List your applications as a page (see the query parameters below). |
| `GET` | `/applications/{id}` | Get one application with its full status history. |
| `PUT` | `/applications/{id}` | Update `company`, `position`, `notes` and `links`. The status is not part of this body; it changes only through the status endpoint. |
| `POST` | `/applications/{id}/status` | Change the status, for example `{ "status": "INTERVIEW" }`. |
| `DELETE` | `/applications/{id}` | Delete an application. Returns `204`. |

`company` and `position` are required. `notes` and `links` are optional and limited to 500 characters each. A request that breaks these rules returns `400`.

Query parameters for `GET /applications`:

- `currentStatus`: filters by current status.
- `company`, `position`: case-insensitive substring search.
- `sort`: one of `applicationId`, `company`, `position`, `currentStatus`, optionally followed by `,asc` or `,desc`. The default is newest first.
- `page`, `size`: pagination, with at most 50 items per page.

Unknown parameters and unsupported sort fields return `400`.

### Status workflow

```
SAVED ──► APPLIED ──► INTERVIEW ──► OFFER
  │          │          │   ▲         │
  │          │          └───┘         │
  └──────────┴──────────┴─────────────┴──► REJECTED
```

`INTERVIEW` can follow `INTERVIEW` to record several interview rounds. An illegal transition returns `409 Conflict`.

## Design decisions

- **Sessions instead of JWT.** The server stores the session, so a logout or forced logout takes effect immediately. The trade-off is that cookie-based authentication needs CSRF protection, which uses Spring Security's SPA cookie and header mode.
- **Status history plus a denormalized current status.** The history is the source of truth. `current_status` is kept in sync inside the aggregate (`Application.changeStatus` is the only way to change it), which makes filtering and sorting cheap and lets the `@Version` check catch concurrent status changes.
- **Ownership enforced in the repository queries.** There's no "load, then check the owner" step that could be forgotten. Integration tests with two users verify the isolation.
- **Paging in SQL.** The list query pages the applications *before* joining their histories, and `fail_on_pagination_over_collection_fetch` is enabled so in-memory pagination can never slip in unnoticed.
- **Real Postgres in tests.** The migrations rely on `uuidv7()` and `citext`, so the tests run on PostgreSQL 18 via Testcontainers instead of H2. The schema under test is exactly the production schema, migrated from scratch.
- **The tested jar is the shipped jar.** CI runs `clean verify`, then builds the Docker image from that same artifact. Images are tagged with the commit SHA (for reproducible deploys) and `latest`.

## CI/CD

The [GitHub Actions workflow](.github/workflows/main.yml) runs on every push and pull request to `main`:

1. `./mvnw clean verify`, which runs all tests including the Testcontainers integration tests.
2. A Docker image built from the tested jar, on every run.
3. On pushes to `main` only, the image is pushed to `ghcr.io/lollopie/applicationtracker`, tagged with the short commit SHA and `latest`.

## Known limitations

- There's no rate limiting or brute-force protection on `/login` and `/register`.
- Sessions are stored in memory, so a restart logs everyone out. Spring Session JDBC would fix that.
- No public deployment exists at the moment. The image is deployable to any container host that provides a PostgreSQL 18 database.

## License

[MIT](LICENSE)
