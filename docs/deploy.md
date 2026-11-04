# Deploying with Docker Compose

This runs OpenWalrus on your own server: one container for the app (the JSON API and the
web client) and, unless you point it at a MongoDB you already have, one for MongoDB.

If an AI assistant is doing the deployment for you, give it
[deploy-agent.md](deploy-agent.md) instead: the same steps as a checklist, each with a
check to pass before moving on.

## What you get

- **The app container** on port 9000, built from this repository.
- **A MongoDB container** with one volume, `openwalrus_mongo_data`, holding everything:
  accounts, grunts and uploaded images. It is not published to the network; only the app
  can reach it. You can remove it and use your own MongoDB instead (below).
- **Restarts by itself** after a crash, and after the server reboots.
- **A health check** that passes only when the app is up and MongoDB answers it.
- **Locked down**: the app runs as a non-root user (id 1000), on a read-only filesystem
  apart from a scratch `/tmp`, with no Linux capabilities and capped log files.

## Quick start

```bash
git clone https://github.com/maximx1/openwalrus.git && cd openwalrus
```

```bash
cp template.env.docker .env
```

Edit `.env` and set the two required values:

```
APP_URL=https://walrus.example.com
APP_SECRET=<output of: openssl rand -hex 32>
```

```bash
docker compose up -d --build
```

The first build takes several minutes: it downloads Node, sbt and every dependency. Later
builds reuse them.

```bash
docker compose ps
```

After about half a minute `app` should read `healthy`. It is now on `127.0.0.1:9000`,
reachable only from the server itself until you put a reverse proxy in front (below).

## Settings

All of them go in `.env`, next to `compose.yaml`.

| Setting | Required | What it does |
| --- | --- | --- |
| `APP_URL` | Yes | The address the app is opened at, exactly as in the browser. Requests for any other host are refused. With `https://` the session cookie is sent over HTTPS only |
| `APP_SECRET` | Yes | Signs session cookies. Anyone who knows it can sign in as any user. Changing it signs everyone out |
| `ALLOW_REGISTRATION` | No | `false` stops new sign-ups. Default `true` |
| `MONGO_URI` | No | The MongoDB to use. Default: the bundled `mongo` service, `mongodb://mongo:27017/openwalrus` |
| `APP_PORT` | No | Port on the server. Default `9000` |
| `APP_BIND` | No | Who can reach that port. Default `127.0.0.1` (this machine only) |
| `APP_DOMAIN` | No | Domain for the bundled Caddy proxy |

Settings are read when the container starts, so after changing `.env` run
`docker compose up -d`. Nothing is baked into the image.

## The database

### The bundled MongoDB (the default)

`compose.yaml` runs `mongo:9` as the `mongo` service, with its data in the volume
`openwalrus_mongo_data`. Nothing needs setting up; the app finds it at
`mongodb://mongo:27017/openwalrus`.

The volume survives rebuilding and replacing both containers. It does **not** survive
`docker compose down -v` or `docker volume rm`.

### Using your own MongoDB

If the server already runs MongoDB for other things, or you would rather run it yourself:

1. In `compose.yaml`, delete the `mongo:` service (the block between the two
   `# ----` lines) and the `mongo_data:` line under `volumes:` at the bottom.
2. In `.env`, set `MONGO_URI` to its connection string. The database name is the part
   after the last `/`; it is created on first use.

   A MongoDB running directly on this machine (not in Docker) is `host.docker.internal`
   from inside the container. It must listen on the Docker bridge, not only on 127.0.0.1
   (`net.bindIp` in `mongod.conf`):

   ```
   MONGO_URI=mongodb://openwalrus:<password>@host.docker.internal:27017/openwalrus?authSource=admin
   ```

   A MongoDB in another Compose project on this machine: join its network instead. Add
   to the `app` service in `compose.yaml`

   ```yaml
       networks: [default, shared]
   ```

   and at the bottom

   ```yaml
   networks:
     shared:
       external: true
       name: <that project's network, from: docker network ls>
   ```

   then use its service name as the host: `MONGO_URI=mongodb://mongo:27017/openwalrus`.

   A MongoDB elsewhere on the network: its address, as you would give any client.

3. `docker compose up -d`, then `docker compose ps`. If `app` doesn't become healthy,
   `docker compose logs app` shows why; an unreachable database shows as a timeout.

The app needs read and write access to its one database, nothing else. A user for it:

```js
// in mongosh, connected as an admin
use admin
db.createUser({ user: "openwalrus", pwd: "<password>", roles: [{ role: "readWrite", db: "openwalrus" }] })
```

To go back to the bundled one, restore the block in `compose.yaml` (`git checkout
compose.yaml`) and remove `MONGO_URI` from `.env`. Moving data between the two is a
`mongodump` from one and `mongorestore` into the other.

### Data from the 2015 version

The collection names (`persons`, `grunts`, `imageset`, and the `owimages` GridFS bucket)
and their fields are the old ones, so an old database reads as it is: point `MONGO_URI`
at it, or `mongorestore` a dump of it into the bundled MongoDB. Old mentions still show on
the mentioned person's profile. The one thing that doesn't carry over is the "phone or
email" each account was made with, which is no longer asked for or shown.

## Reverse proxy and HTTPS

Over plain `http://` the password and session travel unencrypted, and with an `https://`
`APP_URL` the session cookie isn't sent at all. Pick one.

### A. You already run a reverse proxy

Add a site that forwards to `127.0.0.1:9000`.

Caddy:

```
walrus.example.com {
	reverse_proxy 127.0.0.1:9000
}
```

nginx:

```nginx
server {
    listen 443 ssl;
    server_name walrus.example.com;
    # ssl_certificate / ssl_certificate_key as for your other sites

    location / {
        proxy_pass http://127.0.0.1:9000;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        # Picture uploads are up to 10 MB.
        client_max_body_size 12m;
    }
}
```

The proxy must pass the `Host` header through unchanged: the app answers only requests
for the host in `APP_URL`.

### B. Use the bundled Caddy

For a server with no proxy yet and a domain whose DNS points at it. Ports 80 and 443 must
be reachable from the internet so Caddy can obtain a certificate.

```bash
echo 'APP_DOMAIN=walrus.example.com' >> .env
```

```bash
docker compose --profile caddy up -d
```

### C. Keep it private with Tailscale

```bash
sudo tailscale serve --bg 9000
```

Put the `https://….ts.net` address it prints in `APP_URL` and run `docker compose up -d`.

### Trying it first without HTTPS

On your home network, before any of that, use these in `.env`:

```
APP_URL=http://yourserver:9000
APP_BIND=0.0.0.0
```

and open `http://yourserver:9000`. Don't leave it like this.

## Create your accounts, then close the door

Open the address, choose **Sign up**, and have everyone in your group do the same. Then
stop anyone else from registering: in `.env` set

```
ALLOW_REGISTRATION=false
```

```bash
docker compose up -d
```

## Updating

```bash
deploy/backup.sh /var/backups/openwalrus
```

```bash
git pull
```

```bash
docker compose up -d --build
```

There are no migrations: the app reads what's there and adds indexes it is missing.

## Backups

```bash
deploy/backup.sh /var/backups/openwalrus
```

That writes `openwalrus-<date>-<time>.archive.gz`, a `mongodump` of the whole database
(images included), safely while the app is running. It works with the bundled MongoDB;
with your own, back that up the way you already do.

To restore one into the bundled MongoDB, replacing what's there:

```bash
docker compose exec -T mongo mongorestore --drop --archive --gzip < /var/backups/openwalrus/openwalrus-<date>-<time>.archive.gz
```

To run backups nightly with systemd, create `/etc/systemd/system/openwalrus-backup.service`:

```ini
[Unit]
Description=Back up OpenWalrus

[Service]
Type=oneshot
WorkingDirectory=/opt/openwalrus
ExecStart=/opt/openwalrus/deploy/backup.sh /var/backups/openwalrus
```

and `/etc/systemd/system/openwalrus-backup.timer`:

```ini
[Unit]
Description=Nightly OpenWalrus backup

[Timer]
OnCalendar=daily
Persistent=true

[Install]
WantedBy=timers.target
```

```bash
sudo systemctl enable --now openwalrus-backup.timer
```

## Looking at what it is doing

```bash
docker compose logs -f app
```

```bash
docker compose ps
```

## When something is off

| Symptom | Likely cause |
| --- | --- |
| `Set APP_URL in .env …` or `Set APP_SECRET …` when starting | `.env` is missing or lacks that line |
| Every page is a bare 400 "Host not allowed" | The address in the browser isn't the host in `APP_URL`, or the proxy rewrites `Host` |
| You sign in and are signed straight back out | `APP_URL` is `https://` but you opened plain `http://` |
| "Too many wrong passwords for that handle" | Ten wrong passwords for that handle in 15 minutes. It clears by itself after that, or at once with `docker compose restart app` |
| `app` stays `unhealthy` | `docker compose logs app`. A timeout talking to MongoDB means `MONGO_URI` is wrong or the database is unreachable |
| Port 9000 already in use | Another app (LeCarton is also a Play app) has it. Set `APP_PORT` to a free one |
| `compose build requires buildx` | Install `docker-buildx` |

## What is in the image

- Ubuntu with the Eclipse Temurin 21 JRE, and the app as `sbt stage` lays it out.
- The web client, built with Node 22 during the image build; Node itself isn't in the
  final image.
- Runs as the unprivileged `ubuntu` user (id 1000). Java's heap is capped at 256 MB
  (`JAVA_OPTS` in the Dockerfile), plenty for a small group.
