import sbt._

abstract class LWJGLProject(info: ProjectInfo) extends DefaultProject(info) {
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
      case _ => (path("lib") / "lwjgl" / "native" / defineOs._1, defineOs._2)
    }

    System.getProperty("java.library.path") + separator + libpath
  }

  override def fork = {
    forkRun(("-Djava.library.path=" + nativeLWJGLPath) :: Nil)
  }
}

/**
 * Adding other dependencies are really easy now
 */
trait Slick2D extends LWJGLProject {
  val slickRepo = "Slick2D Maven Repo" at "http://slick.cokeandcode.com/mavenrepo"

  // Mainly for slick stuff
  val b2srepo = "Personal Repo" at "http://b2s-repo.googlecode.com/svn/trunk/mvn-repo"

  val slick = "slick" % "slick" % "274"
}
