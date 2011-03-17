import sbt._
<<<<<<< HEAD
import java.io._
import java.util.jar._

object JarExtractor {
	private def copyStream(istream: InputStream, ostream: OutputStream) : Unit = {
		var bytes =  new Array[Byte](1024)
		var len = -1
		while({ len = istream.read(bytes, 0, 1024); len != -1 })
		ostream.write(bytes, 0, len)
	}

	def extractJar(file: File, directory: String) : Unit = {
		val basename = file.getName.substring(0, file.getName.lastIndexOf("."))
		val todir = if(directory == "") new File(file.getParentFile, basename) else new File(directory + "/" + basename)
		todir.mkdirs()

		println("Extracting " + file + " to " + todir)
		val jar = new JarFile(file)
		val enu = jar.entries
		while(enu.hasMoreElements) {
			val entry = enu.nextElement
			val entryPath = if(entry.getName.startsWith(basename)) entry.getName.substring(basename.length)
				else entry.getName

			println("Extracting to " + todir + "/" + entryPath)
			if(entry.isDirectory) {
				new File(todir, entryPath).mkdirs
			} else {
				val istream = jar.getInputStream(entry)
				val ostream = new FileOutputStream(new File(todir, entryPath))
				copyStream(istream, ostream)
				ostream.close
				istream.close
			}
		}
	}

	def apply(filename: String): Unit = extractJar(new File(filename), "")

	def apply(filename: String, directory: String): Unit = extractJar(new File(filename), directory)
}

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
=======
import scala.io.Source

abstract class LWJGLProject(info: ProjectInfo) extends DefaultProject(info) { 
  def lwjglVersion = "2.6"

  lazy val lwjglRepo = "Diablo-D3" at "http://adterrasperaspera.com/lwjgl"

  lazy val lwjgl = "org.lwjgl" % "lwjgl" % lwjglVersion
  lazy val lwjglUtils = "org.lwjgl" % "lwjgl-util" % lwjglVersion
  lazy val lwjglPath = "lwjgl-native-%s" format(lwjglVersion)

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
      case _ => (path("lib") / lwjglPath / defineOs._1, defineOs._2)
    }

    System.getProperty("java.library.path") + separator + libpath
  }

  override def fork = {
    val dir = dependencyPath / lwjglPath
    if(dir.exists) {
      log.info("Skipping because of existence: %s" format(dir))
    } else {
      FileUtilities.unzip(managedDependencyPath / "compile" / "%s.jar".format(lwjglPath), dir, log)
    }
    forkRun(("-Djava.library.path=" + nativeLWJGLPath) :: Nil)
  }
>>>>>>> 1767012ede7a1a5e3b929c558993b2ca0b09180f
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
