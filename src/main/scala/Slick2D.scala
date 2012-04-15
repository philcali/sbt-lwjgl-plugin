import sbt._

import Keys._
import io.Source

/**
 * Slick dependencies
 */
object Slick2D extends Plugin {

  object slick {
    val version = SettingKey[String]("slick-version")

    val localJnlp = TaskKey[Unit]("slick-local-jnlp",
      "If the freehep repo is down, then write jnlp to ivy cache.")

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

  private def localJnlpTask = (streams, ivyPaths) map { (s, ivys) =>
    val base = ivys.ivyHome.getOrElse(Path.userHome / ".ivy2")

    val jnlpBase = base / "cache" / "javax.jnlp" / "jnlp"

    val jnlp = jnlpBase / "ivy-1.2.xml"

    if (jnlp.exists) {
      s.log.info("jnlp dependency already exists in cache.")
    } else {
      val x = Helpers.ivyMe("javax.jnlp", "jnlp", "1.2", "jnlp", "20051013174638")

      val jar = jnlpBase / "jars" / "jnlp-1.2.jar"

      IO.write(jnlp, x.toString)
      IO.transfer(getClass.getResourceAsStream("/jnlp-1.2.jar"), jar)

      s.log.info("jnlp installed.")
    }
  }

  lazy val baseSettings: Seq[Setting[_]] = Seq (
    slick.version := "274",

    slick.patch <<= slickPatchTask,

    slick.localJnlp <<= localJnlpTask,

    update <<= update dependsOn (slick.patch, slick.localJnlp),

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
