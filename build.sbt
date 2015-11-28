name := """openwalrus"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  cache,
  ws,
  filters,
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.5.play24",
  "org.mongodb" %% "casbah" % "2.8.2",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "org.json4s" %% "json4s-ext" % "3.3.0",
  "joda-time" % "joda-time" % "2.4",
  "org.joda" % "joda-convert" % "1.6",
  "com.github.nscala-time" %% "nscala-time" % "2.4.0",
  "com.github.tototoshi" %% "play-json4s-jackson" % "0.4.2",
  "com.github.tototoshi" %% "play-json4s-test-jackson" % "0.4.2" % "test",
  "org.webjars" %% "webjars-play" % "2.4.0-1",
  "org.webjars" % "jquery" % "2.1.4",
  "org.webjars" % "bootstrap" % "3.3.5",
  "io.github.gitbucket" % "markedj" % "1.0.4",
  "org.jsoup" % "jsoup" % "1.8.3",
  "org.scalatestplus" %% "play" % "1.4.0-M3" % "test",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % "test",
  "com.github.simplyscala" %% "scalatest-embedmongo" % "0.2.2" % "test"
)

routesGenerator := InjectedRoutesGenerator

resolvers ++= Seq(
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  "amateras-repo" at "http://amateras.sourceforge.jp/mvn/"
)

TwirlKeys.templateImports += "models._"

resolvers += Resolver.url("Typesafe Ivy releases", url("https://repo.typesafe.com/typesafe/ivy-releases"))(Resolver.ivyStylePatterns)

fork in run := false

coverageExcludedPackages := "<empty>;views.*;router;controllers.*;core.filters.*;core.localization.Constants;models.json.Jsonify;"

coverageMinimum := 80

coverageFailOnMinimum := true
