import sbt._

import java.util.regex.Pattern
import java.io.{ FileNotFoundException, FileOutputStream }

import Keys._
import Project.Initialize

object LWJGLPlugin extends Plugin {
  import lwjgl._

  object lwjgl {
    /** LWJGL Settings */
    val version = SettingKey[String]("lwjgl-version")

    val copyDir = SettingKey[File]("lwjgl-copy-directory", 
      "This is where lwjgl resources will be copied")

    val nativesDir = SettingKey[File]("lwjgl-natives-directory", 
      "This is the location where the lwjgl-natives will bomb to") 

    val os = SettingKey[(String, String)]("lwjgl-os", 
      "This is the targeted OS for the build. Defaults to the running OS.")

    /** LWJGL Tasks */ 
    val copyNatives = TaskKey[Seq[File]]("lwjgl-copy-natives", 
      "Copies the lwjgl library from natives jar to managed resources")

    val manifestNatives = TaskKey[Unit]("lwjgl-manifest-natives", 
      "Copy LWJGL resources to output directory")
  }

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
  def defineOs = System.getProperty("os.name").toLowerCase.take(3).toString match {
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

  lazy val lwjglSettings: Seq[Setting[_]] = baseSettings ++ runSettings

  lazy val baseSettings: Seq[Setting[_]] = Seq (
    lwjgl.version := "2.7.1",
    nativesDir <<= (target) { _ / "lwjgl-natives" }, 

    manifestNatives <<= lwjglNativesTask,
    manifestNatives <<= manifestNatives dependsOn update,

    resolvers += "Diablo-D3" at "http://adterrasperaspera.com/lwjgl",
    libraryDependencies <++= (lwjgl.version) { v => Seq(
      "org.lwjgl" % "lwjgl" % v, 
      "org.lwjgl" % "lwjgl-util" % v 
    ) }
  )

  lazy val runSettings: Seq[Setting[_]] = Seq (
    os := defineOs,
    copyDir <<= (resourceManaged in Compile) { _ / "lwjgl-resources" },

    copyNatives <<= lwjglCopyTask,
    resourceGenerators in Compile <+= copyNatives,

    cleanFiles <+= copyDir,

    fork := true,
    javaOptions <+= (copyDir, lwjgl.os) { (dir, os) => 
      "-Djava.library.path=%s".format(dir / os._1)
    }
  )
}
