import sbt._

import Keys._
import LWJGLKeys._

object Ardor3D {
  lazy val engineSettings: Seq[Setting[_]] = LWJGLProject.engineSettings ++ Seq (

    version in Ardor := "0.8-SNAPSHOT",

    resolvers ++= Seq (
      "Ardor3D Maven repo" at 
      "http://ardor3d.com:8081/nexus/content/groups/public/",
      "Ardor3D Third party" at 
      "http://ardor3d.com:8081/nexus/content/repositories/thirdparty",
      "Google Repo" at
      "http://google-maven-repository.googlecode.com/svn/repository/"
    ),

    libraryDependencies <+= (version in Ardor) { 
      "com.ardor3d" % "ardor3d" % _
    }
  )
}
