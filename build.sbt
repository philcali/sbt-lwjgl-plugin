sbtPlugin := true

name := "sbt-lwjgl-plugin"

organization := "com.github.philcali"

version <<= (sbtVersion) ("sbt" + _ + "_3.0.4") 

publishTo := Some("Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishArtifact in (Compile, packageBin) := true

publishArtifact in (Test, packageBin) := false

publishArtifact in (Compile, packageDoc) := false

publishArtifact in (Compile, packageSrc) := false
