import sbt._

class SbtLWGJLPluginProject(info: ProjectInfo) extends PluginProject(info) {
  override def managedStyle = ManagedStyle.Maven
  val publishTo = "Scala Tools Nexus" at
                  "http://nexus.scala-tools.org/content/repositories/releases/"

  Credentials(Path.userHome / ".ivy2" / ".credentials", log)
}
