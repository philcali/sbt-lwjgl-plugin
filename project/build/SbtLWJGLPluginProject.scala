import sbt._

class SbtLWGJLPluginProject(info: ProjectInfo) extends PluginProject(info) {
  // Using dispatch module to pull down "certain" sources
  val disVersion = "0.7.8"
  val dispatch = "net.databinder" %% "dispatch-http" % disVersion

  override def managedStyle = ManagedStyle.Maven
  val publishTo = "Scala Tools Nexus" at
                  "http://nexus.scala-tools.org/content/repositories/releases/"

  Credentials(Path.userHome / ".ivy2" / ".credentials", log)
}
