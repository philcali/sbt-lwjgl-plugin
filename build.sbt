sbtPlugin := true

scalacOptions += "-deprecation"

name := "sbt-lwjgl-plugin"

organization := "com.github.philcali"

version := "3.1.2"

libraryDependencies += "net.databinder" %% "dispatch-http" % "0.8.5"

publishTo := Some("Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
