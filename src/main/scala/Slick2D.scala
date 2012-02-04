import sbt._

import Keys._
import io.Source

/**
 * Slick dependencies
 */
object Slick2D extends Plugin {

  object slick {
    val version = SettingKey[String]("slick-version")

    val patch = TaskKey[Unit]("slick-patch", 
      "The phys2d dependency pom is broken. Patch aims to fix it")
  }

  private def slickPatchTask = (streams, ivyPaths) map { (s, ivys) =>
    val base = ivys.ivyHome.getOrElse(Path.userHome / ".ivy2")

    val path = base / "cache" / "phys2d" / "phys2d" / "ivy-060408.xml"

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

  lazy val baseSettings: Seq[Setting[_]] = Seq (
    slick.version := "274",

    slick.patch <<= slickPatchTask,

    update <<= update dependsOn slick.patch,

    resolvers ++= Seq (
      "Slick2D Maven Repo" at "http://slick.cokeandcode.com/mavenrepo",
      "b2srepo" at "http://b2s-repo.googlecode.com/svn/trunk/mvn-repo",
      "Freehep" at "http://java.freehep.org/maven2"
    ),
    libraryDependencies <+= (slick.version) {
      "slick" % "slick" % _
    }
  )

  lazy val slickSettings: Seq[Setting[_]] = 
    LWJGLPlugin.lwjglSettings ++ baseSettings
}
