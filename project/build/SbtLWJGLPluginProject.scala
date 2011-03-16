import sbt._

class SbtLWGJLPluginProject(info: ProjectInfo) extends PluginProject(info) {
  val testVersion = "1.1"
  val scalatest = "org.scalatest" % "scalatest" % testVersion % "test" 
}
