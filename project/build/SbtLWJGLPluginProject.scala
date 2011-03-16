import sbt._

class SbtLWGJLPluginProject(info: ProjectInfo) extends PluginProject(info) {
	override def managedStyle = ManagedStyle.Maven
	lazy val publishTo = Resolver.file("GitHub Pages", new java.io.File("../scan.github.com/maven/"))
}
