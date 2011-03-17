import sbt._
import java.util.regex.Pattern
import java.io.FileNotFoundException
import scala.io.Source

abstract class LWJGLProject(info: ProjectInfo) extends DefaultProject(info) { 
	def lwjglVersion = "2.7.1"

	lazy val lwjglRepo = "Diablo-D3" at "http://adterrasperaspera.com/lwjgl"

	lazy val lwjgl = "org.lwjgl" % "lwjgl" % lwjglVersion
	lazy val lwjglUtils = "org.lwjgl" % "lwjgl-util" % lwjglVersion
	lazy val lwjglPath = "lwjgl-native-%s" format(lwjglVersion)

	private lazy val nativeLibPath = dependencyPath / lwjglPath

	private lazy val defineOs = System.getProperty("os.name").toLowerCase.take(3).toString match {
		case "lin" => ("linux", ":", "so")
		case "mac" => ("macosx", ":", "lib")
		case "win" => ("windows", ";", "dll")
		case "sun" => ("solaris", ":", "so")
		case _ => ("unknown", "", "")
	}

	def nativeLWJGLPath = {
		val (libpath, separator) = defineOs._1 match {
		case "unknown" => ("", "")
		case _ => (nativeLibPath / defineOs._1, defineOs._2)
		}

		System.getProperty("java.library.path") + separator + libpath
	}

	override def fork = {
		try {
			if(nativeLibPath.exists) {
				log.info("Skipping because of existence: %s" format(nativeLibPath))
			} else {
				val filter = new PatternFilter(Pattern.compile(defineOs._1 + "/.*" + defineOs._3))
				FileUtilities.unzip(managedDependencyPath / "compile" / "%s.jar".format(lwjglPath), nativeLibPath, filter, log)
			}
			forkRun(("-Djava.library.path=" + nativeLWJGLPath) :: Nil)
		} catch {
			case e: FileNotFoundException => {
				log.error("%s not found, try sbt update.".format(lwjglPath))
				None
			}
		}
	}
}

// Slick works a tad differently
trait Slick2D extends LWJGLProject {
	val slickRepo = "Slick2D Maven Repo" at "http://slick.cokeandcode.com/mavenrepo"

	// Mainly for slick stuff
	val b2srepo = "b2srepo" at "http://b2s-repo.googlecode.com/svn/trunk/mvn-repo"
	val freeheprepo = "Freehep" at "http://java.freehep.org/maven2"

	val slick = "slick" % "slick" % "274"

	override def updateAction = {
		patchFile
		super.updateAction
	}

	private def patchFile = {
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
		case false =>
			log.warn("Update might fail. This is expected.")
			log.warn("Please run update one more time.")
		}
	}
}
