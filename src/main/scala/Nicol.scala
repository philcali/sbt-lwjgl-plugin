import sbt._

import Keys._

object Nicol {

  lazy val engineSettings = LWJGLProject.engineSettings ++ Seq (
    version in LWJGLKeys.Nicol := "0.1.0.1",

    libraryDependencies <+= (version in LWJGLKeys.Nicol) {
      "com.github.scan" %% "nicol" % _
    }
  )
}
