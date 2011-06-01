import sbt._

import java.util.regex.Pattern
import java.io.{ FileNotFoundException, FileOutputStream }

abstract class LWJGLProject(info: ProjectInfo) extends DefaultProject(info) { 
  lazy val compilePath = managedDependencyPath / "compile"

  lazy val lwjglRepo = "Diablo-D3" at "http://adterrasperaspera.com/lwjgl"

	lazy val lwjgl = "org.lwjgl" % "lwjgl" % lwjglVersion
	lazy val lwjglUtils = "org.lwjgl" % "lwjgl-util" % lwjglVersion
 
	private lazy val defineOs = System.getProperty("os.name").toLowerCase.take(3).toString match {
		case "lin" => ("linux", ":", "so")
		case "mac" | "dar" => ("macosx", ":", "lib")
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
				FileUtilities.unzip(compilePath / "%s.jar".format(lwjglJar), nativeLibPath, filter, log)
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

  lazy val lwjglNatives = task {
    val unzipTo = lwjglNativeOutputPath / "natives"
    val lwjglN = compilePath / "%s.jar".format(lwjglJar)

    FileUtilities.unzip(lwjglN, unzipTo, log)

    val allFiles = unzipTo ** "*.*"
    FileUtilities.copyFlat(allFiles.get, lwjglNativeOutputPath, log)
    FileUtilities.clean(unzipTo, log)
    unzipTo.asFile.delete
    None
  } dependsOn(`update`) describedAs "Extract lwjgl natives to defined outputPath."

  // Override this to extract libraries somewhere else
  def lwjglNativeOutputPath = outputPath
	def lwjglJar = "lwjgl-native-%s".format(lwjglVersion)
	def lwjglVersion = "2.7.1"

	override def copyResourcesAction = super.copyResourcesAction dependsOn copyLwjgl

  // Removing the java.library.path addition, as
  // this could only cause the double loading
  // error... Will revisit this if this implementation
  // becomes a problem (though I don't see how it could).
	def nativeLWJGLPath = defineOs._1 match {
		case "unknown" => ""
		case _ => nativeLibPath / defineOs._1
	}
        
	override def fork = {
		forkRun(("-Djava.library.path=" + nativeLWJGLPath) :: Nil)
	}

}
