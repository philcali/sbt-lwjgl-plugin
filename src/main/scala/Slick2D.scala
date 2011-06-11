import sbt._

import Keys._
import io.Source

/**
 * Slick dependencies
 */
object Slick2D extends Plugin {
  val slickVersion = SettingKey[String]("slick-version", "The version of Slick2D in the Maven Repo")

  val slickPatch = TaskKey[Unit]("slick-patch", "The phys2d dependency pom is broken. Patch aims to fix it")
  private def slickPatchTask = (streams) map { s =>
  }

  lazy val engineSettings = Seq (
    slickVersion := "274", 
    slickPatch <<= slickPatchTask, 
    resolvers ++= Seq (
      "Slick2D Maven Repo" at "http://slick.cokeandcode.com/mavenrepo",
      "b2srepo" at "http://b2s-repo.googlecode.com/svn/trunk/mvn-repo",
      "Freehep" at "http://java.freehep.org/maven2"
    ),
    libraryDependencies <+= (slickVersion) {
      "slick" % "slick" % _
    }
  )
}
/*
trait Slick2D extends LWJGLProject { 
  // Patch unfortunately can't depend on update because
  // update will fail leaving the file to be patched behind
	lazy val `patch` = task {
		val path = "%s/.ivy2/cache/phys2d/phys2d/ivy-060408.xml" format(System.getProperty("user.home"))
		new java.io.File(path) exists match {
		case true =>
			log.info("Patching %s ..." format(path))
			val pattern = "zip".r
			val ivysource = Source.fromFile(path)
			val text = ivysource.getLines.mkString
			val writer = new java.io.FileWriter(path)
			writer.write(pattern.replaceAllIn(text, "jar"))
			writer.close
			log.info("Done.")
      None
		case false =>
			log.warn("Update might fail. This is expected.")
			log.warn("Please run update one more time.")
      None
		}
	} describedAs "Patchs the phys2d dependency xml file"

	override def updateAction = 
		super.updateAction dependsOn `patch`

}
*/
