OpenWalrus
==========

A small, self-hosted Twitter for a group of friends, in the spirit of the early one: sign
up, post short messages ("grunts", in Markdown, with pictures and videos), mention people
with `@handle`, follow each other, and read a feed of the people you follow. Grunts can be
replied to and regrunted, kept to your friends (the people you follow), and the ones
getting the most attention this week are on the Hot page.

Started in 2015 on Play 2.4; brought up to date in 2026.

## How it's built

- **Server**: Scala 3, Play 3, a JSON API under `/api` (`conf/routes`). Data access goes
  through the traits in `app/repositories/Repositories.scala`; the MongoDB versions are in
  `app/repositories/mongo/` and are chosen in `app/Module.scala`. Another database would
  be another set of implementations and a change to that binding.
- **Database**: MongoDB, connected with the official Scala driver. Pictures and videos
  are stored in GridFS. The connection comes from `MONGO_URI`.
- **Web client**: React + TypeScript, built with Vite, in `web/`. It builds into
  `public/`, which Play serves; every path that isn't the API gets `index.html`.

## Running it on a server

Docker Compose, with MongoDB included or pointed at your own: see
[docs/deploy.md](docs/deploy.md).

## Developing

You need JDK 21 or later, sbt, Node 22 or later, and a MongoDB:

```bash
docker run -d --name openwalrus-mongo -p 127.0.0.1:27017:27017 mongo:9
```

The server, on port 9000 (it uses `mongodb://localhost:27017/openwalrus` unless `MONGO_URI`
says otherwise):

```bash
sbt run
```

The web client, on port 5173, passing `/api` through to the server:

```bash
cd web && npm install && npm run dev
```

Open http://localhost:5173.

### Tests

```bash
sbt test
```

The API tests run against a real MongoDB, in a throwaway database, only when
`TEST_MONGO_URI` is set:

```bash
TEST_MONGO_URI=mongodb://localhost:27017 sbt test
```

The web client has a type check:

```bash
cd web && npm run typecheck
```

## License

MIT, see [LICENSE](LICENSE).
