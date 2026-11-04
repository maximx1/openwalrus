name := "openwalrus"

version := "2.0.0-SNAPSHOT"

scalaVersion := "3.9.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  guice,
  filters,
  "org.mongodb.scala" %% "mongo-scala-driver" % "5.14.0",
  // Argon2id for new password hashes; jBCrypt still checks accounts made before it.
  "org.bouncycastle" % "bcprov-jdk18on" % "1.80",
  "org.mindrot" % "jbcrypt" % "0.4",
  "org.jsoup" % "jsoup" % "1.24.1",
  "io.github.gitbucket" % "markedj" % "1.0.21",
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % Test
)

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

// The web client is built by Vite into public/ (see web/). Nothing else is served from there.
PlayKeys.playDefaultPort := 9000
