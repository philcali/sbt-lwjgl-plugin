import sbt._

import java.util.regex.Pattern
import java.io.{ FileNotFoundException, FileOutputStream }

import Keys._
import Project.Initialize
import Defaults._

// Base LWJGL support
object LWJGLProject extends Plugin {
  // Default Settings
  val lwjglCopyDir = SettingKey[File]("lwjgl-copy-location", "This is where lwjgl resources will be copied")
  val lwjglNativesDir = SettingKey[File]("lwjgl-natives-directory", "This is the location where the lwjgl-natives will bomb to") 
  val lwjglVersion = SettingKey[String]("lwjgl-version", "This is the targeted LWJGL verision")
  val lwjglOs = SettingKey[(String, String)]("lwjgl-os", "This is the targeted OS for the build. Defaults to the running OS.")

  // Define Tasks
  lazy val lwjglCopy = TaskKey[Seq[File]]("lwjgl-copy", "Copies the lwjgl library from natives jar to managed resources")
  private def lwjglCopyTask: Initialize[Task[Seq[File]]] = 
    (streams, lwjglCopyDir, lwjglVersion, lwjglOs) map { (s, dir, lwv, dos) =>
      val (os, ext) = dos 
      s.log.info("Copying files for %s" format(os))

      val target = dir / os

      if(target.exists) {
        s.log.info("Skipping because of existence: %s" format(target))
        Nil
      } else {
        val filter = new PatternFilter(Pattern.compile(os + "/.*" + ext))

        IO.unzip(pullNativeJar(lwv), dir.asFile, filter)
  
        // Return the managed LWJGL resources
        target * "*" get
      }
    }

  val lwjglClean = TaskKey[Unit]("lwjgl-clean", "Clean the LWJGL resource dir")
  private def lwjglCleanTask: Initialize[Task[Unit]] =
    (streams, lwjglCopyDir, lwjglOs) map { (s, dir, os) =>
      s.log.info("Cleaning LWJGL files")
      IO.delete(dir / os._1)
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

  lazy val engineSettings = Seq (
    // Settings
    lwjglVersion := "2.7.1",
    lwjglCopyDir <<= (resourceManaged in Compile) { _ / "lwjgl-resources" },
    lwjglNativesDir <<= (target) { _ / "lwjgl-natives" }, 
    lwjglOs := defineOs,

    // Tasks and dependencies
    lwjglCopy <<= lwjglCopyTask,
    resourceGenerators in Compile <+= lwjglCopy.identity,
    lwjglNatives in update <<= lwjglNativesTask,
    lwjglNatives <<= Seq(update, lwjglNatives in update).dependOn,
    lwjglClean <<= lwjglCleanTask,
    cleanFiles <+= lwjglCopyDir.identity,

    // Neeed to load LWJGL in java.library.path
    fork := true,
    javaOptions <+= (lwjglCopyDir, lwjglOs) { (dir, os) => 
      "-Djava.library.path=%s".format(dir / os._1)
    },
    
    // Project Dependencies
    resolvers += "Diablo-D3" at "http://adterrasperaspera.com/lwjgl",
    libraryDependencies <++= (lwjglVersion) { v => Seq(
      "org.lwjgl" % "lwjgl" % v, 
      "org.lwjgl" % "lwjgl-util" % v 
    ) }
  )
}
