# spring-url-shortener

A compact URL shortener written in Java 21 and Spring Boot. It creates short links, optional custom aliases, optional expiry, and click counts, then redirects visitors to the original URL.

The original version of this project was a small Spring Boot service co-developed with [Milad Tehrany](https://github.com/MiladTehrany). This repository is a ground-up rewrite of that idea: PostgreSQL (with H2 for local use), Flyway, stronger URL checks, and a documented HTTP API.

## Features

- Random base62 short codes, retried until the code is unused
- Optional custom aliases (`docs`, `launch-2026`, ...)
- Optional expiry (`expiresInDays` or `expiresAt`)
- Atomic click counting on each redirect
- Validation: only `http` and `https`, no credentials in the URL, optional host blocklist, optional rejection of private and loopback hosts
- Configurable 301 or 302 redirects (302 is the default, so browsers keep sending visits)
- Simple in-memory rate limiting on create and delete
- OpenAPI UI at `/swagger-ui`
- RFC 7807 problem details for errors
- Flyway migrations, multi-stage Docker image, and a Compose stack for PostgreSQL

The API is unauthenticated. Run it on a trusted network or put your own access control in front of it.

## Requirements

- Java 21
- Maven 3.9+ (the Maven Wrapper is included)
- Docker and Docker Compose, if you want the PostgreSQL stack

## Quick start

### Local (in-memory H2)

```bash
./mvnw spring-boot:run
```

The default `dev` setup uses an in-memory H2 database and listens on `http://localhost:8080`. OpenAPI UI: `http://localhost:8080/swagger-ui`.

### PostgreSQL with Docker Compose

```bash
cp .env.example .env
docker compose up --build
```

Compose starts PostgreSQL and the application with the `prod` profile. The default database password in `docker-compose.yml` is for local use only; change it before exposing the stack.

## API examples

Create a short URL:

```bash
curl -sS -X POST http://localhost:8080/api/v1/urls \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://example.com/articles/hello"}'
```

```json
{
  "code": "aB3kQ9m",
  "shortUrl": "http://localhost:8080/aB3kQ9m",
  "url": "https://example.com/articles/hello",
  "createdAt": "2026-11-22T18:00:00Z",
  "expiresAt": null,
  "clickCount": 0
}
```

Create one with a custom alias and a seven-day expiry:

```bash
curl -sS -X POST http://localhost:8080/api/v1/urls \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://example.com/docs","alias":"docs","expiresInDays":7}'
```

Follow the redirect:

```bash
curl -sSI http://localhost:8080/docs
```

Inspect click counts and expiry (this does not increment the counter):

```bash
curl -sS http://localhost:8080/api/v1/urls/docs
```

Delete a short URL:

```bash
curl -sS -X DELETE http://localhost:8080/api/v1/urls/docs
```

Errors use `application/problem+json`. Typical statuses:

- `400` invalid URL or request body
- `404` unknown code
- `409` alias already taken or reserved (`api`, `swagger-ui`, ...)
- `410` the short URL has expired
- `429` write rate limit exceeded

## Configuration

Values can be set in `application.yml` or with environment variables. See `.env.example` for a complete list.

| Variable | Default | Meaning |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | HTTP port |
| `SPRING_PROFILES_ACTIVE` | *(none, uses H2)* | Set `prod` for PostgreSQL |
| `SHORTURL_BASE_URL` | `http://localhost:8080` | Prefix used when building `shortUrl` |
| `SHORTURL_CODE_LENGTH` | `7` | Length of generated base62 codes |
| `SHORTURL_MAX_CODE_ATTEMPTS` | `8` | Collision retries before giving up |
| `SHORTURL_REDIRECT_STATUS` | `302` | `301` or `302` |
| `SHORTURL_BLOCK_PRIVATE_HOSTS` | `true` | Reject loopback, private, and link-local hosts |
| `SHORTURL_BLOCKED_HOSTS` | *(empty)* | Comma-separated host blocklist |
| `SHORTURL_RATE_LIMIT_ENABLED` | `true` | Rate-limit POST and DELETE under `/api/` |
| `SHORTURL_RATE_LIMIT_RPM` | `60` | Allowed writes per client per minute |
| `SHORTURL_RATE_LIMIT_TRUST_FORWARDED_HEADERS` | `false` | Use `X-Forwarded-For` only behind a trusted proxy |
| `SPRING_DATASOURCE_URL` | | JDBC URL (required in `prod`) |
| `SPRING_DATASOURCE_USERNAME` | | Database user (required in `prod`) |
| `SPRING_DATASOURCE_PASSWORD` | | Database password (required in `prod`) |

## Design notes

- Each create call allocates a new code, even if the same target URL was shortened before, so aliases and expiry stay independent.
- Codes are generated with `SecureRandom` over the base62 alphabet. The service checks the database (and a reserved-word list) before insert, and a unique constraint remains the last line of defence.
- The host block for private addresses inspects literal IP addresses and well-known local names. It does not resolve public DNS names, so it is not a complete SSRF defence on its own.
- Rate limiting is an in-memory fixed window and is per process. Use an edge limiter if you run more than one instance.
- Schema changes go through Flyway (`src/main/resources/db/migration`). Hibernate is set to `validate`.

## Tests

```bash
./mvnw verify
```

The `verify` phase runs unit tests, MockMvc integration tests against H2, and a JaCoCo coverage gate (80% line, 70% branch). Continuous integration is in `.github/workflows/ci.yml`.

## Credits

The original version was co-developed with [Milad Tehrany](https://github.com/MiladTehrany).

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE).
