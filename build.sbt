sbtPlugin := true

name := "xsbt-lwjgl-plugin"

organization := "com.github.philcali"

version := "0.0.1"

publishTo := Some("Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishMavenStyle := true
