import sbt._

trait Nicol extends LWJGLProject {
  def nicolVersion = "0.1"

  lazy val nicol = "com.github.scan" %% "nicol" % nicolVersion 
}
