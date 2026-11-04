# Deployment instructions for an AI assistant

You are deploying **OpenWalrus** on a Linux server with Docker Compose. Follow the steps in
order. Each step ends with a **Check**; do not go on until it passes.

If a check fails, stop, show the owner the command, its output and the last 100 lines of
`docker compose logs app`, and wait. Do not improvise around a failure.

## Rules

1. **Never run `docker compose down -v`, `docker volume rm`, or `docker system prune
   --volumes`.** The volume `openwalrus_mongo_data` holds the database: every account,
   grunt and picture. Losing it loses everything.
2. **Do not edit the application's code or `Dockerfile`.** Configuration goes in `.env`.
   The one change to `compose.yaml` this document allows is removing the `mongo` service
   in step 3, and only if the owner chose their own MongoDB.
3. **Do not touch nginx or any other reverse proxy.** The owner handles that. Step 8 says
   what to report so they can.
4. **Do not print the contents of `.env` back or commit it.** It holds the secret that signs
   sessions, and maybe a database password. It is ignored by git; leave it that way.
5. Do not expose port 9000 to the network. It stays on `127.0.0.1`; the proxy is the only
   way in.
6. Use `sudo` only where a step says so.

## What you need from the owner first

Ask for these if you were not given them:

| Name | Meaning | Example |
| --- | --- | --- |
| `DOMAIN` | The address the app will be opened at, without `https://` | `walrus.mysite.com` |
| `APP_DIR` | Where the code goes | `/opt/openwalrus` |
| `BACKUP_DIR` | Where nightly database copies go | `/var/backups/openwalrus` |
| `BRANCH` | The branch to deploy. `master` unless the owner says otherwise | `master` |
| `DATABASE` | `bundled` (the MongoDB in `compose.yaml`) or a MongoDB connection string of their own | `bundled` |

If `DATABASE` is a connection string, ask the owner for it directly, put it straight into
`.env` in step 3, and never repeat it back: it may contain a password.

## Step 1: Docker is installed and starts at boot

```bash
docker --version && docker compose version && docker buildx version
```

If any of the three is missing, install them. On Arch:

```bash
sudo pacman -S --needed docker docker-compose docker-buildx
```

On Debian or Ubuntu, install Docker Engine with the compose and buildx plugins from
Docker's own repository, following docs.docker.com for that distribution.

```bash
sudo systemctl enable --now docker
```

**Check:** all three version commands print a version, and

```bash
systemctl is-enabled docker && systemctl is-active docker
```

prints `enabled` and `active`.

If `docker` commands fail with "permission denied" on the socket, either prefix them with
`sudo` for the rest of this document or ask the owner whether to add the user to the
`docker` group.

## Step 2: Get the code

```bash
sudo mkdir -p APP_DIR && sudo chown "$USER": APP_DIR
git clone --branch BRANCH https://github.com/maximx1/openwalrus.git APP_DIR
```

If `APP_DIR` already holds a clone, update it instead:

```bash
git -C APP_DIR pull --ff-only origin BRANCH
```

Run every later command from inside `APP_DIR`.

**Check:** `ls` shows `compose.yaml`, `Dockerfile`, `template.env.docker` and `deploy/`.

## Step 3: Configure

```bash
cp -n template.env.docker .env
chmod 600 .env
```

Set the address, exactly as it will appear in a browser: `https://`, the domain, no
trailing slash, no path.

```bash
sed -i 's|^APP_URL=.*|APP_URL=https://DOMAIN|' .env
```

Generate the session secret straight into the file, without showing it. Do this **once**:
a new secret signs everybody out. Skip it if `APP_SECRET=` already has a value.

```bash
grep -q '^APP_SECRET=.\+' .env || sed -i "s|^APP_SECRET=.*|APP_SECRET=$(openssl rand -hex 32)|" .env
```

**Only if `DATABASE` is the owner's own MongoDB:** delete the `mongo:` service from
`compose.yaml` (the block between the two `# ----` lines) and the `mongo_data:` line under
`volumes:` at the bottom, then add the connection string to `.env` with an editor (not
with `echo`, which would put it in the shell history) as

```
MONGO_URI=<the connection string>
```

A MongoDB running directly on this server is `host.docker.internal` from inside the
container. "Using your own MongoDB" in `docs/deploy.md` covers the other cases.

Make sure nothing else is using the port:

```bash
ss -ltn | grep ':9000 ' || echo "9000 is free"
```

If it is taken (LeCarton, another Play app, also defaults to 9000), add `APP_PORT=9001`
(or another free port) to `.env` and use that number wherever this document says 9000.

**Check:** both of these print `1`:

```bash
grep -c '^APP_URL=https://[^/]*$' .env
grep -c '^APP_SECRET=[0-9a-f]\{64\}$' .env
```

and `docker compose config -q` prints nothing.

## Step 4: Build and start

```bash
docker compose up -d --build
```

The first build downloads Node, sbt and every dependency, then builds the app; allow ten
minutes.

Wait for it to report healthy (up to two minutes):

```bash
for i in $(seq 1 24); do s=$(docker inspect -f '{{.State.Health.Status}}' "$(docker compose ps -q app)"); echo "$s"; [ "$s" = healthy ] && break; sleep 5; done
```

**Check:** the last line printed is `healthy`, and all of these succeed:

```bash
curl -fsS http://127.0.0.1:9000/api/health
```

prints `{"status":"ok"}` (the server answers and so does the database).

```bash
curl -fsS -o /dev/null -w '%{http_code} %{content_type}\n' -H 'Host: DOMAIN' http://127.0.0.1:9000/
```

prints a line starting `200 text/html`.

```bash
curl -s -o /dev/null -w '%{http_code}\n' -H 'Host: somewhere-else.example' http://127.0.0.1:9000/
```

prints `400`: requests for any other host are refused.

```bash
docker compose exec -T app sh -c 'touch /app/should-fail 2>&1; touch /tmp/.write-test && rm /tmp/.write-test && echo tmp-writable'
```

prints a "Read-only file system" error for the first file, then `tmp-writable`.

If the container keeps restarting or never becomes healthy, stop here and report
`docker compose ps` and `docker compose logs app --tail 100`.

## Step 5: The data survives the containers

Skip this step if the owner uses their own MongoDB.

Everything is in the `openwalrus` database of the `mongo` service, kept in the volume
`openwalrus_mongo_data`. Replacing the containers must not replace it.

```bash
docker compose exec -T mongo mongosh --quiet openwalrus --eval 'db.recreate_test.insertOne({v: "survives"}).acknowledged'
docker compose up -d --force-recreate
```

Wait for `healthy` as in step 4, then:

```bash
docker compose exec -T mongo mongosh --quiet openwalrus --eval 'print(db.recreate_test.findOne().v); db.recreate_test.drop()'
```

**Check:** it prints `survives` (and then `true`, from dropping the test collection).

## Step 6: It comes back by itself

```bash
docker inspect -f '{{.HostConfig.RestartPolicy.Name}}' "$(docker compose ps -q app)"
```

must print `unless-stopped`.

Now simulate a crash. Use the host's `kill` on the container's main process. Do **not** use
`docker kill` or `docker stop` for this test: Docker treats those as "stopped on purpose"
and deliberately does not restart.

```bash
c=$(docker compose ps -q app); sudo kill -9 "$(docker inspect -f '{{.State.Pid}}' "$c")"
```

Wait for `healthy` as in step 4, then:

```bash
docker inspect -f '{{.RestartCount}} {{.State.Health.Status}}' "$(docker compose ps -q app)"
```

**Check:** it prints a restart count of `1` or more followed by `healthy`.

| Event | Result |
| --- | --- |
| The server process crashes or is killed | Docker starts it again within seconds |
| The machine reboots | Docker starts at boot (step 1) and starts both containers |
| MongoDB is down for a while | The app turns `unhealthy` and answers errors, then recovers by itself when MongoDB is back |
| Someone runs `docker compose stop` | It stays stopped until `docker compose start`. That is intended |
| It is running but frozen, not answering | The health check turns `unhealthy`. Docker reports that but does not act on it |

## Step 7: Nightly backups

Skip this step if the owner uses their own MongoDB, and tell them in step 8 that it is not
backed up by this setup.

```bash
sudo mkdir -p BACKUP_DIR && sudo chown "$USER": BACKUP_DIR
deploy/backup.sh BACKUP_DIR
```

**Check:** it prints `Saved BACKUP_DIR/openwalrus-<date>-<time>.archive.gz`, and that file
exists and is not empty. It holds the whole database, pictures included.

Schedule it. Create `/etc/systemd/system/openwalrus-backup.service` (replace `APP_DIR`,
`BACKUP_DIR` and `USERNAME` with the real values; `USERNAME` is the user who can run
`docker`):

```ini
[Unit]
Description=Back up OpenWalrus
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
User=USERNAME
WorkingDirectory=APP_DIR
ExecStart=APP_DIR/deploy/backup.sh BACKUP_DIR
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
sudo systemctl daemon-reload && sudo systemctl enable --now openwalrus-backup.timer
sudo systemctl start openwalrus-backup.service
```

**Check:** `systemctl is-enabled openwalrus-backup.timer` prints `enabled`, and a second
`openwalrus-*.archive.gz` file has appeared in `BACKUP_DIR`.

Tell the owner: these copies are on the same server as the app. They protect against a bad
update or a mistake, not against losing the server. They also pile up; nothing deletes old
ones.

## Step 8: Report to the owner

Stop here and report. The owner sets up DNS and the reverse proxy; you do not. Tell them:

- The app is listening on `http://127.0.0.1:9000` on this server (or the port chosen in
  step 3), reachable only from the server itself.
- The proxy needs a site for `DOMAIN` with a TLS certificate that forwards everything to
  that address and passes the `Host` header through unchanged (the app refuses any other
  host). Picture uploads are up to 10 MB, so nginx needs `client_max_body_size 12m;`.
  `docs/deploy.md` has a complete nginx and Caddy example.
- The site must be served over `https://`. With an `https://` `APP_URL`, signing in does
  not work over plain `http://`: the session cookie is marked secure.
- If the proxy runs inside Docker rather than on the host, `127.0.0.1` will not reach the
  app from it. Say so; that needs a decision from the owner.
- **Anyone who finds the address can sign up** until `ALLOW_REGISTRATION=false` is set in
  `.env` (then `docker compose up -d`). The owner will want that once their group has
  joined.
- A new database starts with no accounts.
- Which database it uses (bundled or theirs), and the results of steps 4 to 7, including
  the restart count from the crash test.

## Step 9: After the proxy is in place

When the owner says the domain is live:

```bash
curl -fsS https://DOMAIN/api/health
```

**Check:** prints `{"status":"ok"}`, with no certificate warning.

```bash
curl -fsS -o /dev/null -w '%{http_code} %{content_type}\n' https://DOMAIN/
```

**Check:** a line starting `200 text/html`.

Then the owner opens `https://DOMAIN` in a browser, signs up, posts a grunt with a picture,
signs out and signs back in. Ask them to confirm that each of those worked and that the
picture shows in the grunt.

## Updating later

```bash
deploy/backup.sh BACKUP_DIR
git pull --ff-only origin BRANCH
docker compose up -d --build
```

Wait for `healthy` as in step 4. If it does not become healthy, report the logs. The
backup just taken is the way back:

```bash
docker compose exec -T mongo mongorestore --drop --archive --gzip < BACKUP_DIR/openwalrus-<date>-<time>.archive.gz
```

Run that only if the owner asks for it: it replaces everything in the database.

## If something is wrong

| Symptom | Cause |
| --- | --- |
| `Set APP_URL in .env …` or `Set APP_SECRET in .env …` when starting | `.env` is missing or that line is empty (step 3) |
| `service "app" depends on undefined service` or a similar compose error | `compose.yaml` was edited beyond removing the `mongo` block. `git diff compose.yaml` and report |
| `compose build requires buildx` | `docker-buildx` is not installed (step 1) |
| `app` exits with code 126 straight away | The image is broken. Report it; do not edit the Dockerfile |
| `app` stays `unhealthy`, logs show timeouts talking to MongoDB | `MONGO_URI` is wrong or that MongoDB is unreachable from the container |
| Every page is a plain 400 | The request's host isn't the one in `APP_URL`, or the proxy rewrites `Host` |
| Signs in, then is signed out straight away | The site is being opened over `http://` with an `https://` `APP_URL` |
| Everyone was signed out after a restart | `APP_SECRET` changed or is missing from `.env` |
| Picture upload fails with `413 Request Entity Too Large` | nginx needs `client_max_body_size 12m;` (step 8) |
| `502 Bad Gateway` from the proxy | The container is not running or not on the port the proxy forwards to: `docker compose ps` |

The longer explanation of every part is in `docs/deploy.md`.
