sbtPlugin := true

name := "sbt-lwjgl-plugin"

organization := "com.github.philcali"

version := "3.0.1"

publishTo := Some("Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishMavenStyle := true
