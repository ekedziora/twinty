name := "twinty"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "org.webjars" %% "webjars-play" % "2.4.0-1",
  "org.webjars" % "jquery-ui" % "1.11.4",
  "org.webjars" % "underscorejs" % "1.5.2-2",
  "com.adrianhurt" %% "play-bootstrap" % "1.0-P25-B3"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
