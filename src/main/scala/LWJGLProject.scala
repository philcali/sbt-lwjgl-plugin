import sbt._

import scala.io.Source
import java.net.URL
import java.util.regex.Pattern
import java.io.{ FileNotFoundException, FileOutputStream }

abstract class LWJGLProject(info: ProjectInfo) extends DefaultProject(info) { 
	lazy val lwjglRepo = "Diablo-D3" at "http://adterrasperaspera.com/lwjgl"

	lazy val lwjgl = "org.lwjgl" % "lwjgl" % lwjglVersion
	lazy val lwjglUtils = "org.lwjgl" % "lwjgl-util" % lwjglVersion
 
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
	def lwjglJar = "lwjgl-native-%s" format(lwjglVersion)
	def lwjglVersion = "2.7.1"

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

/**
 * JMonkey, unfortunately, does not have a public maven repo
 * We are going to hack the dependency by pulling down their
 * nightly builds and extracting the dependecies we need.
 */
trait JMonkey extends LWJGLProject {
  lazy val baseRepo = "http://jmonkeyengine.com/nightly" 
  lazy val jname = "%s_%s" format(jmonkeyBaseVersion, targetedVersion)

  // Bulk of the work, any exception here can
  // bubble up to the updateAction
  lazy val updateJmonkey = task {
    dependencyPath / jname exists match {
      case true => 
        log.info("Already have %s" format(jname))
        None
      case false =>
        // If they wanted a nightly build then this could get extreme
        log.info("Cleaning older versions of %s" format(jmonkeyBaseVersion))
        val previousVersions = dependencyPath * "%s*".format(jmonkeyBaseVersion) 
        FileUtilities.clean(previousVersions.get, log)

        // Start the download
        log.info("Pulling %s" format(jname))
        log.warn("This may take a few minutes...")
        val zip = "%s.zip" format(jname) 
        val dest = dependencyPath / jname
        
        // Comencing work...
        val zipFile = new java.io.File(zip)
        val url = new URL("%s/%s" format(baseRepo, zip))
        FileUtilities.download(url, zipFile, log) 
        // Extract the lib dir only...
        val filter = new PatternFilter(Pattern.compile(".*jar"))
        FileUtilities.unzip(zipFile, dest, filter, log)
        // Destroy the zip
        zipFile.delete
        log.info("Complete")
        None
    } 
  } describedAs "Pulls jMonkey dependency from nightly build."

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

  override def updateAction = 
    super.updateAction dependsOn updateJmonkey 
}

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
