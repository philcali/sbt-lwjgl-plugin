import sbt._

import Keys._

object Ardor3D {
  // Settings
  val ardorVersion = SettingKey[String]("ardor-version", "Ardor3D version in the Maven repo")

  lazy val engineSettings = Seq (
    ardorVersion := "0.8-SNAPSHOT",
    resolvers ++= Seq (
      "Ardor3D Maven repo" at 
      "http://ardor3d.com:8081/nexus/content/groups/public/",
      "Ardor3D Third party" at 
      "http://ardor3d.com:8081/nexus/content/repositories/thirdparty",
      "Google Repo" at
      "http://google-maven-repository.googlecode.com/svn/repository/"
    ),
    libraryDependencies <+= (ardorVersion) { 
      "com.ardor3d" % "ardor3d" % _
    }
  )
}
