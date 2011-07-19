import sbt._

import Keys._
import LWJGLKeys._
import io.Source

/**
 * Slick dependencies
 */
object Slick2D {
  private def slickPatchTask = (streams) map { s =>
    val path = Path.userHome / ".ivy2" / "cache" / "phys2d" / "phys2d" / "ivy-060408.xml"
    if (path.exists) {
			s.log.info("Patching %s ..." format(path))
			val pattern = "zip".r
			val ivysource = Source.fromFile(path)
			val text = ivysource.getLines.mkString
			val writer = new java.io.FileWriter(path)
			writer.write(pattern.replaceAllIn(text, "jar"))
			writer.close
			s.log.info("Done.")
    } else {
			s.log.warn("Update might fail. This is expected.")
			s.log.warn("Please run update one more time.")
    }
  }

  lazy val engineSettings: Seq[Setting[_]] = LWJGLProject.engineSettings ++ Seq (
    slickVersion := "274", 
    slickPatch <<= slickPatchTask, 
    update <<= update dependsOn slickPatch,
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
