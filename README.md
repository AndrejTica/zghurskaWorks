# Artist Portfolio

A Spring Boot portfolio with public artwork collections, commissions, and exhibitions plus a protected studio admin page.

## Manage collections

In `/admin`, create a collection with a name and description. Use the collection selector when adding or editing an artwork to assign it to one collection (or leave it unassigned). Edit the collection to choose one of its assigned artworks as the preview shown beside its name in the public **Collections** tab. Visitors can open a collection to read its description and browse all its artworks.

Choose the **Highlighted collection** in the manager to control the homepage gallery. Only artworks in that collection appear there, including commissioned artworks assigned to it. The separate **Commissions** tab continues to show all commissioned works. Without a highlighted collection, the homepage gallery displays an empty state; existing artworks are retained and remain unassigned until you categorize them. The landing-page hero image remains independently configurable.

Deleting a collection keeps its artworks and uploaded images, removes their collection assignment, and clears the homepage highlight if necessary. Moving or deleting a preview artwork clears that collection's preview and prompts you to select a replacement. Collections without a preview display a placeholder.

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
