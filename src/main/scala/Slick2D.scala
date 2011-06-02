import sbt._

import io.Source

/**
 * Slick dependencies
 */
trait Slick2D extends LWJGLProject { 
	lazy val slickRepo = "Slick2D Maven Repo" at "http://slick.cokeandcode.com/mavenrepo"

	// Mainly for slick stuff
	lazy val b2srepo = "b2srepo" at "http://b2s-repo.googlecode.com/svn/trunk/mvn-repo"
	lazy val freeheprepo = "Freehep" at "http://java.freehep.org/maven2"

	lazy val slick = "slick" % "slick" % slickVersion 

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

  // Override this for newer version
  def slickVersion = "274"

	override def updateAction = 
		super.updateAction dependsOn `patch`

}
