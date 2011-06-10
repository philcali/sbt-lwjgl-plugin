import sbt._

import Keys._

object Ardor3D extends Plugin {
  // Settings
  val ardorVersion = SettingKey[String]("ardor-version", "Ardor3D version in the Maven repo")

  lazy val engineSettings = Seq (
    ardorVersion := "0.7",
    resolvers += "Ardor3D Maven Repo" at 
                 "http://ardor3d.com:8081/nexus/content/groups/public/",
    libraryDependencies <+= (ardorVersion) { 
      "com.ardor3d" % "ardor3d" % _
    }
  )
}
