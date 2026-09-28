# Artist Portfolio

A Spring Boot portfolio with a public artwork, commission, and exhibition site plus a protected studio admin page.

## Run with Docker

Docker and Docker Compose are the only requirements.

Build and start the application:

```bash
docker compose up --build -d
```

Open <http://localhost:8080> for the portfolio and <http://localhost:8080/admin> for the protected admin page. The default local admin credentials are `artist` / `change-me-local`.

For anything beyond local development, set your own credentials before starting:

```bash
export ADMIN_USERNAME='artist'
export ADMIN_PASSWORD='choose-a-strong-admin-password'
docker compose up --build -d
```

Stop the stack with:

```bash
docker compose down
```

Database records are stored in an embedded H2 database. The database and uploaded images are retained in the `portfolio-data` Docker volume. JPEG, PNG, WebP, and GIF uploads are accepted up to 10 MB.
