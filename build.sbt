name := """openwalrus"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  cache,
  ws,
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % "test",
  "org.scalatestplus" %% "play" % "1.4.0-M3" % "test",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.5.play24",
  "org.mongodb" %% "casbah" % "2.8.2",
  "com.github.simplyscala" %% "scalatest-embedmongo" % "0.2.2" % "test"
)

routesGenerator := InjectedRoutesGenerator

resolvers ++= Seq(
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
)

resolvers += Resolver.url("Typesafe Ivy releases", url("https://repo.typesafe.com/typesafe/ivy-releases"))(Resolver.ivyStylePatterns)

fork in run := true

coverageExcludedPackages := "<empty>;"

coverageMinimum := 80

coverageFailOnMinimum := true
