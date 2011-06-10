import sbt._

import java.util.regex.Pattern
import java.io.{ FileNotFoundException, FileOutputStream }

import Keys._
import Project.Initialize
import Defaults._

// Base LWJGL support
object LWJGLProject extends Plugin {
  // Default Settings
  val lwjglCopyDir = SettingKey[RichFile]("lwjgl-copy-location", "This is where lwjgl resources will be copied")
  val lwjglNativesDir = SettingKey[RichFile]("lwjgl-natives-directory", "This is the location where the lwjgl-natives will bomb to") 
  val lwjglVersion = SettingKey[String]("lwjgl-version", "This is the targeted LWJGL verision")

  // Define Tasks
  val lwjglCopy = TaskKey[Unit]("lwjgl-copy", "Copies the LWJGL files needed to run in lwjgl-copy-location")
  private def lwjglCopyTask: Initialize[Task[Unit]] = 
    (streams, lwjglCopyDir, lwjglVersion) map { (s, dir, lwv) =>
      val (os, ext) = defineOs
      s.log.info("Copying files for %s" format(os))

      val target = dir / os

      if(target.exists) {
        s.log.info("Skipping because of existence: %s" format(target))
      } else {
        val filter = new PatternFilter(Pattern.compile(os + "/.*" + ext))

        IO.unzip(pullNativeJar(lwv), dir.asFile, filter)
      }
    }

  val lwjglClean = TaskKey[Unit]("lwjgl-clean", "Clean the LWJGL resource dir")
  private def lwjglCleanTask: Initialize[Task[Unit]] =
    (streams, lwjglCopyDir) map { (s, dir) =>
      s.log.info("Cleaning LWJGL files")
      IO.delete(dir / defineOs._1 asFile)
    }

  val lwjglNatives = TaskKey[Unit]("lwjgl-natives", "Copy LWJGL resources to output directory")
  private def lwjglNativesTask =
    (streams, lwjglNativesDir, lwjglVersion) map { (s, outDir, lwv) =>
      val unzipTo = file(".") / "natives-cache"
      val lwjglN = pullNativeJar(lwv)

      s.log.info("Unzipping the native jar")
      IO.unzip(lwjglN, unzipTo)

      val allFiles = unzipTo ** "*.*"

      allFiles.get foreach { f =>
        IO.copyFile(f, outDir / f.name)
      }
      // Delete cache
      s.log.info("Removing cache")
      IO.delete(unzipTo.asFile)
    }

  // Helper methods 
  private def defineOs = System.getProperty("os.name").toLowerCase.take(3).toString match {
    case "lin" => ("linux", "so")
    case "mac" | "dar" => ("macosx", "lib")
    case "win" => ("windows", "dll")
    case "sun" => ("solaris", "so")
    case _ => ("unknown", "")
  }

  private def pullNativeJar(lwv: String) = { 
    val org = "org.lwjgl"
    val name = "lwjgl-native"
    val jar = "%s-%s.jar" format(name, lwv)
    Path.userHome / ".ivy2" / "cache" / org / name / "jars" / jar
  }

  // Every project must have these
  override lazy val settings = Seq (
    // Settings
    lwjglVersion := "2.7.1",
    lwjglCopyDir := file(".") / "lwjgl-resources",
    lwjglNativesDir <<= (target) { _ / "lwjgl-natives" }, 

    // Tasks and dependencies
    lwjglCopy in update <<= lwjglCopyTask,
    lwjglCopy <<= Seq(update, lwjglCopy in update).dependOn,
    lwjglNatives in update <<= lwjglNativesTask,
    lwjglNatives <<= Seq(update, lwjglCopy, lwjglNatives in update).dependOn,
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
