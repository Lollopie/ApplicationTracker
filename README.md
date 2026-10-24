# Job Application Tracker API

A REST API for tracking job applications through their lifecycle, from saved to applied, interview, offer or rejected. Each user sees only their own applications. Every status change is recorded in a history, and the allowed transitions are enforced by the domain model.

Built as a learning and portfolio project, with a focus on doing the backend fundamentals properly: authentication, data isolation, concurrency, testing and delivery.

## Tech stack

- **Java 21**, **Spring Boot 4.1** (Web MVC, Data JPA, Validation, Security 7, Actuator)
- **PostgreSQL 18** with **Flyway** migrations, Hibernate 7
- **Testcontainers**, JUnit 5, MockMvc, `RestTestClient`
- **Docker** (layered, non-root image) and **GitHub Actions** CI publishing to GitHub Container Registry

## Features

- **Session-based authentication:** JSON login, server-side sessions stored in PostgreSQL with Spring Session JDBC (so they survive restarts), session fixation protection, and BCrypt behind a delegating password encoder. The password hash is erased from the session's principal after login.
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

The API is then available at `http://localhost:8080`, and the web frontend at `http://localhost:3000`. You can check the API with `GET /actuator/health`.

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

## Frontend

A React single-page app lives in [`frontend/`](frontend): Vite, React 19, TypeScript, React Router, TanStack Query and Tailwind CSS. It covers registration, login, the filterable and sortable application list, creating and editing applications, the status workflow with its history, and deletion.

The SPA calls the API under `/api`, and a proxy strips that prefix before forwarding to Spring. The browser only ever talks to one origin, so the session and CSRF cookies work without any CORS configuration, and the SPA's routes (`/login`, `/applications/...`) never collide with the API's paths. The Vite dev server is the proxy during development, and nginx is the proxy in the Compose setup.

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173, proxies /api to the backend on localhost:8080
npm test           # Vitest + Testing Library + MSW
npm run lint       # oxlint
npm run build      # type-check and production build into dist/
```

Start the backend first (from the IDE, or with `docker compose up postgres spring`).

To serve the app under a path prefix, build it with `VITE_BASE`, for example `VITE_BASE=/tracker/ npm run build`. It then calls the API at `/tracker/api`, and the backend needs `COOKIE_PATH=/tracker`.

On a host that puts the backend to sleep when idle, build with `VITE_SERVER_SLEEPS=true` (as [`render.yaml`](render.yaml) does). The SPA then treats gateway errors as "waking up" and keeps retrying the session check for about two minutes. Without it, a gateway error means the backend isn't running, so the SPA gives up after a few seconds and doesn't mention waking up.

`scripts/e2e-smoke.sh` runs a full register → login → CRUD → status workflow → logout pass against a running stack, through the same paths and cookies the SPA uses. Run it after `docker compose up`.

## API overview

All request and response bodies are JSON. Requests that change state (`POST`, `PUT`, `DELETE`) need a CSRF token.

### Authentication flow

1. `GET /csrf` sets the `XSRF-TOKEN` cookie.
2. `POST /register` with `{ "username": "...", "password": "..." }` returns `201`, or `409` if the username is taken. Usernames are 1 to 32 characters (letters, digits, `.`, `_` and `-`), and passwords are 8 to 64 characters.
3. `POST /login` with the same body returns `200` and a `JSESSIONID` cookie. Login only checks that both fields are present, so tightening the registration rules later can't lock out existing users.
4. The CSRF token changes on login, so call `GET /csrf` again before your next state-changing request.
5. `GET /me` returns `{ "username": "..." }` for the logged-in user, or `401` without a valid session.
6. `POST /logout` ends the session.

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
- **Path-based hosting with scoped cookies.** In production the app shares one domain with other apps (`example.com/tracker/`). The cookie path and the session cookie name are configurable, so the tracker's cookies never reach or collide with the neighbouring apps. The CSRF filter and the token rotation on login share one cookie repository, so both use the same path.

## Deploying to Render

Production runs on Render's free tier, behind one domain shared with other apps:

| Path | Served by |
|---|---|
| `example.com/` | the home page (a Render Static Site in its own repo), which is also the gateway |
| `example.com/tracker/*` | `tracker-web`, the SPA built with `VITE_BASE=/tracker/` into `dist/tracker/` |
| `example.com/tracker/api/*` | `tracker-api`, the Spring image from GHCR, reached through `tracker-web` with the `/tracker/api` prefix stripped |

The home site forwards `/tracker/*` to `tracker-web` unchanged. `tracker-web` forwards `/tracker/api/*` to `tracker-api` and serves the SPA shell for every other path, with the API rule listed first. Both are static sites, so neither ever sleeps. `tracker-web` is also a complete copy of the tracker on its own URL, at `https://<tracker-web>.onrender.com/tracker/`.

Setup:

1. **Database:** create a Neon project on **PostgreSQL 18** in the same region as the API (Frankfurt in [`render.yaml`](render.yaml)). Flyway creates the schema on the first start.
2. **Services:** in Render, create a Blueprint from this repo. It defines both services from [`render.yaml`](render.yaml). When asked, fill in the secrets:
   - `DATABASE_URL` as `jdbc:postgresql://<neon-host>/<db>?sslmode=require`
   - `DATABASE_USER`
   - `DATABASE_PASSWORD`
   - `ADMIN_PASSWORD_HASH`
3. **Image access:** make the GHCR package public, or add a registry credential in Render.
4. **Deploys:** copy `tracker-api`'s deploy hook URL into the GitHub secret `RENDER_DEPLOY_HOOK_URL`. `tracker-web` builds itself on every push that touches `frontend/`.
5. **Routing:** Render may add a suffix to a service's URL (here `tracker-api-2igk`, `tracker-web-y2tx`). Point `tracker-web`'s `/tracker/api/*` rewrite in [`render.yaml`](render.yaml) at the API's actual URL, and the home site's `/tracker/*` rewrite at `tracker-web`'s.

The backend's `COOKIE_PATH` must match the frontend's `VITE_BASE`, without the trailing slash. Check a deployment with:

```bash
BASE=https://example.com APP_PREFIX=/tracker CHECK_HEADERS=0 scripts/e2e-smoke.sh
```

## CI/CD

The [GitHub Actions workflow](.github/workflows/main.yml) runs on every push and pull request to `main`:

1. `./mvnw clean verify`, which runs all tests including the Testcontainers integration tests.
2. In parallel, the frontend is linted, tested and built.
3. A Docker image built from the tested jar, on every run.
4. On pushes to `main` only, the backend image is pushed to `ghcr.io/lollopie/applicationtracker`, tagged with the short commit SHA and `latest`.
5. Then the Render deploy hook deploys that exact SHA tag (skipped when the `RENDER_DEPLOY_HOOK_URL` secret isn't set).

## Known limitations

- There's no rate limiting or brute-force protection on `/login` and `/register`.
- On Render's free tier the API sleeps after 15 idle minutes, and the first request afterwards takes about a minute. The frontend shows a "waking up" message and keeps retrying in the meantime, and sessions survive because they're stored in the database.

## License

[MIT](LICENSE)
