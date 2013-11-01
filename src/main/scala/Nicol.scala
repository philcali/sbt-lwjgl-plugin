import sbt._

import Keys._

object Nicol {

  object nicol {
    val version = SettingKey[String]("nicol-version")
  }

  lazy val baseSettings: Seq[Setting[_]] = Seq (
    nicol.version := "0.1.0.1",

    libraryDependencies <+= (nicol.version) {
      "com.github.scan" %% "nicol" % _
    }
  )

  lazy val nicolSettings: Seq[Setting[_]] =
    LWJGLPlugin.lwjglSettings ++ baseSettings
}
