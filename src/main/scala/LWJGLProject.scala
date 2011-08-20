import sbt._

import java.util.regex.Pattern
import java.io.{ FileNotFoundException, FileOutputStream }

import Keys._
import LWJGLKeys._
import Project.Initialize

// Base LWJGL support
object LWJGLProject extends Plugin {

  // Define Tasks
  private def lwjglCopyTask: Initialize[Task[Seq[File]]] = 
    (streams, copyDir, version, os, ivyPaths) map { (s, dir, lwv, dos, ivys) =>
      val (tos, ext) = dos 
      s.log.info("Copying files for %s" format(tos))

      val target = dir / tos

      if(target.exists) {
        s.log.info("Skipping because of existence: %s" format(target))
        Nil
      } else {
        val filter = new PatternFilter(Pattern.compile(tos + "/.*" + ext))

        IO.unzip(pullNativeJar(lwv, ivys.ivyHome), dir.asFile, filter)
  
        // Return the managed LWJGL resources
        target * "*" get
      }
    }

  private def lwjglNativesTask =
    (streams, nativesDir, version, ivyPaths) map { (s, outDir, lwv, ivys) =>
      val unzipTo = file(".") / "natives-cache"
      val lwjglN = pullNativeJar(lwv, ivys.ivyHome)

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

  private def pullNativeJar(lwv: String, ivyHome: Option[File]) = { 
    val org = "org.lwjgl"
    val name = "lwjgl-native"
    val jar = "%s-%s.jar" format(name, lwv)

    val base = ivyHome.getOrElse(Path.userHome / ".ivy2")

    base / "cache" / org / name / "jars" / jar
  }

  lazy val engineSettings: Seq[Setting[_]] = inConfig(LWJGL) { Seq (
    // Settings
    version := "2.7.1",
    copyDir <<= (resourceManaged in Compile) { _ / "lwjgl-resources" },
    nativesDir <<= (target) { _ / "lwjgl-natives" }, 
    os := defineOs,

    // Tasks and dependencies
    copyNatives <<= lwjglCopyTask,
    resourceGenerators in Compile <+= copyNatives.identity,

    targetNatives <<= lwjglNativesTask,

    cleanFiles <+= copyDir.identity
  ) } ++ Seq(
    // Neeed to load LWJGL in java.library.path
    fork := true,
    javaOptions <+= (copyDir in LWJGL, os in LWJGL) { (dir, os) => 
      "-Djava.library.path=%s".format(dir / os._1)
    },
    
    // Project Dependencies
    resolvers += "Diablo-D3" at "http://adterrasperaspera.com/lwjgl",
    libraryDependencies <++= (version in LWJGL) { v => Seq(
      "org.lwjgl" % "lwjgl" % v, 
      "org.lwjgl" % "lwjgl-util" % v 
    ) },

    update <<= update dependsOn (targetNatives in LWJGL)
  )
}
