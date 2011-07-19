import sbt._

import Keys._
import LWJGLKeys.nicolVersion

object Nicol {

  lazy val engineSettings = LWJGLProject.engineSettings ++ Seq (
    nicolVersion := "0.1.0.1",
    libraryDependencies <+= (nicolVersion) {
      "com.github.scan" %% "nicol" % _
    }
  )
}
