import sbt._

import Keys._

object Nicol {
  val nicolVersion = SettingKey[String]("nicol-version", "The version of Nicol in the Maven repo")

  lazy val engineSettings = LWJGLProject.engineSettings ++ Seq (
    nicolVersion := "0.1.0.1",
    libraryDependencies <+= (nicolVersion) {
      "com.github.scan" %% "nicol" % _
    }
  )
}
