import sbt._

class SbtLWGJLPluginProject(info: ProjectInfo) extends PluginProject(info) {
<<<<<<< HEAD
	override def managedStyle = ManagedStyle.Maven
	lazy val publishTo = Resolver.file("GitHub Pages", new java.io.File("../scan.github.com/maven/"))
=======
  override def managedStyle = ManagedStyle.Maven
  val publishTo = "Scala Tools Nexus" at
                  "http://nexus.scala-tools.org/content/repositories/releases/"

  Credentials(Path.userHome / ".ivy2" / ".credentials", log)
>>>>>>> 1767012ede7a1a5e3b929c558993b2ca0b09180f
}
