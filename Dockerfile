# syntax=docker/dockerfile:1
#
# One image that serves both the JSON API and the built web client. MongoDB is not in it:
# compose.yaml runs one alongside, or MONGO_URI points at one you already have.

# ---- build the web client (into /public, where Play serves it from) ----
FROM node:22-bookworm-slim AS web
WORKDIR /web
COPY web/package.json web/package-lock.json ./
RUN npm ci
COPY web/ ./
RUN npm run build

# ---- build the server ----
FROM sbtscala/scala-sbt:eclipse-temurin-21.0.13_9_1.13.1_3.9.0 AS server
WORKDIR /app
# Dependencies first, so changing the code doesn't download them all again.
COPY build.sbt ./
COPY project/build.properties project/plugins.sbt project/
RUN sbt -batch update
COPY app app
COPY conf conf
COPY --from=web /public public
# The packager makes the start script executable by its owner only, and the image runs as
# someone else.
RUN sbt -batch stage && chmod 755 target/universal/stage/bin/openwalrus

# ---- the image that runs ----
FROM eclipse-temurin:21-jre
ENV JAVA_OPTS="-Xms64m -Xmx256m"
WORKDIR /app
COPY --from=server /app/target/universal/stage ./

# Runs as the unprivileged "ubuntu" user (id 1000) that the base image already has. Nothing
# is written to disk except uploads in flight, under /tmp.
USER ubuntu
EXPOSE 9000

# Healthy means the server answers and MongoDB answers it.
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:9000/api/health || exit 1

# The start script execs java, so stop signals reach it. No RUNNING_PID file: the container
# is the process, and the filesystem is read-only.
CMD ["bin/openwalrus", "-Dhttp.port=9000", "-Dpidfile.path=/dev/null"]
