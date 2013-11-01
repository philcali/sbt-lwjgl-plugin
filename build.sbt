sbtPlugin := true

scalacOptions ++= Seq("-feature", "-deprecation")

name := "sbt-lwjgl-plugin"

organization := "com.github.philcali"

version := "3.1.5"

libraryDependencies += "net.databinder.dispatch" %% "dispatch-core" % "0.11.0"

publishTo <<= version { v =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { x => false }

pomExtra := (
  <url>https://github.com/philcali/sbt-lwjgl-plugin</url>
  <licenses>
    <license>
      <name>The MIT License</name>
      <url>http://www.opensource.org/licenses/mit-license.php</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:philcali/sbt-lwjgl-plugin.git</url>
    <connection>scm:git:git@github.com:philcali/sbt-lwjgl-plugin.git</connection>
  </scm>
  <developers>
    <developer>
      <id>philcali</id>
      <name>Philip Cali</name>
      <url>http://philcalicode.blogspot.com/</url>
    </developer>
  </developers>
)
