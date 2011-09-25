sbtPlugin := true

scalacOptions += "-deprecation"

name := "sbt-lwjgl-plugin"

organization := "com.github.philcali"

version := "3.0.6"

publishTo := Some("Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
