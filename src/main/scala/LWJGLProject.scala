import sbt._

abstract class LWJGLProject(info: ProjectInfo) extends DefaultProject(info) {
	def lwjglVersion = "2.6"

	lazy val lwjglRepo = "lwjgl" at "http://adterrasperaspera.com/lwjgl"

	lazy val lwjgl = "org.lwjgl" % "lwjgl" % lwjglVersion
	lazy val lwjglUtil = "org.lwjgl" % "lwjgl-util" % lwjglVersion

	def defineOs = System.getProperty("os.name").toLowerCase.take(3).toString match {
		case "lin" => ("linux", ":")
		case "mac" => ("macosx", ":")
		case "win" => ("windows", ";")
		case "sun" => ("solaris", ":")
		case _ => ("unknown", "")
	}

	def nativeLWJGLPath = {
		val (libpath, separator) = defineOs._1 match {
		case "unknown" => ("", "")
		case _ => (path("lib") / ("lwjgl-native-" + lwjglVersion) / defineOs._1, defineOs._2)
		}

		System.getProperty("java.library.path") + separator + libpath
	}

	override def fork = {
		JarExtractor((managedDependencyPath / "compile" / ("lwjgl-native-" + lwjglVersion + ".jar")).toString, dependencyPath.toString)
		forkRun(("-Djava.library.path=" + nativeLWJGLPath) :: Nil)
	}
}

/**
 * Adding other dependencies are really easy now
 */
trait Slick2D extends LWJGLProject {
  val slickRepo = "Slick2D Maven Repo" at "http://slick.cokeandcode.com/mavenrepo"
  val slick = "slick" % "slick" % "274"
}
