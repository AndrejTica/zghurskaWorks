# Artist Portfolio

A Spring Boot portfolio with a public artwork, commission, and exhibition site plus a protected studio admin page.

## Host with Docker

Docker and Docker Compose are the only requirements.

Point your domain's `A` and/or `AAAA` DNS record to the server. Allow inbound TCP traffic on ports 80 and 443, and UDP traffic on port 443. Do not expose the application port 8080.

Create the deployment configuration:

```bash
cp .env.example .env
```

Edit `.env` and set your domain and a strong admin password:

```dotenv
DOMAIN=portfolio.example.com
ADMIN_USERNAME=artist
ADMIN_PASSWORD=replace-with-a-long-random-password
MAIL_USERNAME=
MAIL_PASSWORD=
CONTACT_RECIPIENT=ann.drago.2002@gmail.com
```

The mail settings are optional. Without both `MAIL_USERNAME` and `MAIL_PASSWORD`, the application starts normally and hides the contact form while keeping the direct email link available.

To enable Gmail delivery, set `MAIL_USERNAME` to the sending Gmail address, enable two-step verification on that Google account, and create an app password. Use that 16-character app password as `MAIL_PASSWORD`, without spaces. Do not use the normal Google account password.

Build and start the application:

```bash
docker compose up --build -d
```

Caddy obtains and renews the HTTPS certificate automatically. Open `https://portfolio.example.com` for the portfolio and `https://portfolio.example.com/admin` for the protected admin page, replacing the example domain with your own.

Stop the stack with:

```bash
docker compose down
```

The Java application is available only to Caddy on the private Docker network. Database records are stored in an embedded H2 database. The database and uploaded images are retained in the `portfolio-data` Docker volume, while Caddy certificates are retained in `caddy-data`. JPEG, PNG, WebP, and GIF uploads are accepted up to 10 MB. The landing-page image can be replaced from the protected admin page. Contact form messages are delivered to `CONTACT_RECIPIENT` through the configured Gmail SMTP account.
