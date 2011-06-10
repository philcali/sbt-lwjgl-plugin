import sbt._

import java.util.regex.Pattern
import java.io.{ FileNotFoundException, FileOutputStream }

import Keys._
import Project.Initialize
import Defaults._

// Base LWJGL support
object LWJGLProject extends Plugin {
  val LWJGL = config("lwjgl")

  // Default Settings
  val lwjglCopyDir = SettingKey[RichFile]("lwjgl-copy-location", "This is where lwjgl resources will be copied")
  val lwjglVersion = SettingKey[String]("lwjgl-version", "This is the targeted LWJGL verision")

  // Define Tasks
  val lwjglCopy = TaskKey[Unit]("lwjgl-copy", "Copies the LWJGL files needed to run in lwjgl-copy-location")
  private def lwjglCopyTask: Initialize[Task[Unit]] = 
    (streams, lwjglCopyDir, lwjglVersion) map { (s, dir, lwv) =>
      val (os, ext) = defineOs
      s.log.info("Copying files for %s" format(os))

      val target = dir / os

      val org = "org.lwjgl"
      val name = "lwjgl-native"
      val jar = "%s-%s.jar" format(name, lwv)

      if(target.exists) {
        s.log.info("Skipping because of existence: %s" format(target))
      } else {
        val filter = new PatternFilter(Pattern.compile(os + "/.*" + ext))

        val targetJar = Path.userHome / ".ivy2" / "cache" / org / name / "jars" / jar
        IO.unzip(targetJar, dir.asFile, filter)
      }
    }

  val lwjglClean = TaskKey[Unit]("lwjgl-clean", "Clean the LWJGL resource dir")
  private def lwjglCleanTask: Initialize[Task[Unit]] =
    (streams, lwjglCopyDir) map { (s, dir) =>
      s.log.info("Cleaning LWJGL files")
      IO.delete(dir / defineOs._1 asFile)
    }

  // Helper methods 
  private def defineOs = System.getProperty("os.name").toLowerCase.take(3).toString match {
    case "lin" => ("linux", "so")
    case "mac" | "dar" => ("macosx", "lib")
    case "win" => ("windows", "dll")
    case "sun" => ("solaris", "so")
    case _ => ("unknown", "")
  }

  // Every project must have these
  override lazy val settings = Seq (
    // Settings
    lwjglCopyDir := file(".") / "lwjgl-resources",
    lwjglVersion := "2.7.1",

    // Tasks and dependencies
    lwjglCopy in update <<= lwjglCopyTask,
    lwjglCopy <<= Seq(update, lwjglCopy in update).dependOn, 
    lwjglClean <<= lwjglCleanTask,

    // Neeed to load LWJGL in java.library.path
    fork := true,
    javaOptions <+= (lwjglCopyDir) { dir => 
      "-Djava.library.path=%s".format(dir / defineOs._1)
    },
    
    // Project Dependencies
    resolvers += "Diablo-D3" at "http://adterrasperaspera.com/lwjgl",
    libraryDependencies <++= (lwjglVersion) { v => Seq(
      "org.lwjgl" % "lwjgl" % v, 
      "org.lwjgl" % "lwjgl-util" % v 
    ) }
  )
}

/*
abstract class LWJGLProject(info: ProjectInfo) extends DefaultProject(info) { 
  lazy val compilePath = managedDependencyPath / "compile"

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

  // Still need to figure out how to do this
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
		forkRun(("-Djava.library.path=" + nativeLWJGLPath) :: Nil
	}
}
*/
