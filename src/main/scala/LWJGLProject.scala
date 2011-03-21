import sbt._
import java.util.regex.Pattern
import java.io.FileNotFoundException
import scala.io.Source

trait LWJGLForkRun extends DefaultProject {

	private lazy val defineOs = System.getProperty("os.name").toLowerCase.take(3).toString match {
		case "lin" => ("linux", ":", "so")
		case "mac" => ("macosx", ":", "lib")
		case "win" => ("windows", ";", "dll")
		case "sun" => ("solaris", ":", "so")
		case _ => ("unknown", "", "")
	}

  // Extracts LWJGL native jar to this location
	private lazy val nativeLibPath = dependencyPath / lwjglJar

	lazy val copyLwjgl = task {
		try {
			log.info("Copying files for %s" format(defineOs._1))
			if(nativeLibPath.exists) {
				log.info("Skipping because of existence: %s" format(nativeLibPath))
			} else {
				val filter = new PatternFilter(Pattern.compile(defineOs._1 + "/.*" + defineOs._3))
				FileUtilities.unzip(managedDependencyPath / "compile" / "%s.jar".format(lwjglJar), nativeLibPath, filter, log)
			}
			None
		} catch {
			case e: FileNotFoundException => {
				Some("%s not found, try sbt update.".format(lwjglJar))
			}
		}
	} describedAs "Copy all LWJGL natives to the right position."

	lazy val cleanLwjgl = task {
		FileUtilities.clean(nativeLibPath, log)
		None
	}

  // Children must override this definition in order
  // for the ForkRun trait to be effective
  def lwjglJar: String

	override def copyResourcesAction = super.copyResourcesAction dependsOn copyLwjgl

	def nativeLWJGLPath = {
		val (libpath, separator) = defineOs._1 match {
		case "unknown" => ("", "")
		case _ => (nativeLibPath / defineOs._1, defineOs._2)
		}

		System.getProperty("java.library.path") + separator + libpath
	}
	override def fork = {
		forkRun(("-Djava.library.path=" + nativeLWJGLPath) :: Nil)
	}
}

abstract class LWJGLProject(info: ProjectInfo) extends DefaultProject(info) with LWJGLForkRun { 
	lazy val lwjglRepo = "Diablo-D3" at "http://adterrasperaspera.com/lwjgl"

	lazy val lwjgl = "org.lwjgl" % "lwjgl" % lwjglVersion
	lazy val lwjglUtils = "org.lwjgl" % "lwjgl-util" % lwjglVersion
 
  // Force override for ForkRun trait 
	def lwjglJar = "lwjgl-native-%s" format(lwjglVersion)
	def lwjglVersion = "2.7.1"
}

/**
 * JMonkey, unfortunately, does not have a public maven repo
 * We are going to hack the dependency by pulling down their
 * nightly builds and extracting the dependecies we need.
 */
import dispatch._
import Http._
import java.io.FileOutputStream

abstract class JMonkeyProject(info: ProjectInfo) extends DefaultProject(info) with LWJGLForkRun {
  lazy val baseRepo = "http://jmonkeyengine.com/nightly" 
  lazy val jname = "%s_%s" format(jmonkeyBaseVersion, targetedVersion)

  // This is the jar with teh goods
  def lwjglJar = "%s-lwjgl-natives" format(jmonkeyBaseVersion)

  // Giving the ability for users to override
  // the base version and targeted nightly build
  def jmonkeyBaseVersion = "jME3"
  def targetedVersion = dateString(today)

  // Plugins are compiled in scala 2.7.7...
  def today = new java.util.Date()
  def dateString(when: java.util.Date) = {
    val sdf = new java.text.SimpleDateFormat("yyyy-MM-dd")
    sdf.format(when)
  }

  // Bulk of the work, any exception here can
  // bubble up to the updateAction
  def pullLib = {
    dependencyPath / jname exists match {
      case true => log.info("Already have targeted jMonkey Version")
      case false =>
        log.info("Pulling new version of jMonkey")
        log.warn("This may take a few minutes...")
        val zip = "%s.zip" format(jname) 
        val dest = dependencyPath / jname
        
        // Comencing work...
        Http("%s/%s".format(baseRepo, zip) >>> new FileOutputStream(zip))
        val zipFile = new java.io.File(zip)
        // Extract the lib dir only...
        val filter = new PatternFilter(Pattern.compile(".*jar"))
        FileUtilities.unzip(zipFile, dest, filter, log)
        // Destroy the zip
        zipFile.delete
        log.info("Complete")
    }
  }

  override def updateAction = { pullLib; super.updateAction }
}

// Slick works a tad differently
abstract class Slick2D(info: ProjectInfo) extends DefaultProject(info) with LWJGLForkRun {
	val slickRepo = "Slick2D Maven Repo" at "http://slick.cokeandcode.com/mavenrepo"

	// Mainly for slick stuff
	val b2srepo = "b2srepo" at "http://b2s-repo.googlecode.com/svn/trunk/mvn-repo"
	val freeheprepo = "Freehep" at "http://java.freehep.org/maven2"

	val slick = "slick" % "slick" % slickVersion 

  // Override this for newer version
  def slickVersion = "274"

  def lwjglJar = "lwjgl-native-2.6"

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
