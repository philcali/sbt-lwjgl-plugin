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

    val org = SettingKey[String]("lwjgl-org",
      "Custom lwjgl maven organization.")

    val nativesName = SettingKey[String]("lwjgl-natives-name",
      "Name of the natives artifact to extract.")

    val nativesJarName = SettingKey[String]("lwjgl-natives-jar-name",
      "This name will be used to pull the specific jar for loading the runtime.")

    val utilsName = SettingKey[String]("lwjgl-utils-name",
      "Name of the utils artifact.")

    val includePlatform = SettingKey[Boolean]("lwjgl-include-platform",
      "Include the platform dependency... Do not if using the old method")

    /** LWJGL Tasks */ 
    val copyNatives = TaskKey[Seq[File]]("lwjgl-copy-natives", 
      "Copies the lwjgl library from natives jar to managed resources")

    val manifestNatives = TaskKey[Unit]("lwjgl-manifest-natives", 
      "Copy LWJGL resources to output directory")
  }

  // Define Tasks
  private def lwjglCopyTask: Initialize[Task[Seq[File]]] = 
    (streams, copyDir, org, nativesName, nativesJarName, os, ivyPaths) map { 
      (s, dir, org, nativesName, jarName, dos, ivys) =>
      val (tos, ext) = dos 
      s.log.info("Copying files for %s" format(tos))

      val target = dir / tos

      if (target.exists) {
        s.log.info("Skipping because of existence: %s" format(target))
        Nil
      } else {
        val nativeLocation = pullNativeJar(org, nativesName, jarName, ivys.ivyHome)

        if (nativeLocation.exists) {
          val filter = new PatternFilter(Pattern.compile(".*" + ext))

          IO.unzip(nativeLocation, target.asFile, filter)
    
          // Return the managed LWJGL resources
          target * "*" get
        } else {
          s.log.warn("""|You do not have the LWJGL natives installed %s.
                        |Consider requiring LWJGL through LWJGLPlugin.lwjglSettings and running
                        |again.""".stripMargin.format(nativeLocation))
          Nil
        }
      }
    }

  private def lwjglNativesTask =
    (streams, nativesDir, org, nativesName, nativesJarName, ivyPaths) map {
      (s, outDir, org, nativesName, jarName, ivys) =>
      val unzipTo = file(".") / "natives-cache"
      val lwjglN = pullNativeJar(org, nativesName, jarName, ivys.ivyHome)

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
    case "mac" | "dar" => ("osx", "lib")
    case "win" => ("windows", "dll")
    case "sun" => ("solaris", "so")
    case _ => ("unknown", "")
  }

  private def pullNativeJar(org: String, name: String, jarName: String, ivyHome: Option[File]) = { 
    val correct = (f: File) =>
      f.getName == "%s.jar".format(jarName)

    val base = ivyHome.getOrElse(Path.userHome / ".ivy2")

    val jarBase = base / "cache" / org / name / "jars"
    val jars = jarBase * "*.jar"

    jars.get.filter(correct).headOption.getOrElse {
      throw new java.io.FileNotFoundException(
        "No Natives found in: %s" format(jarBase)
      )
    }
  }

  lazy val lwjglSettings: Seq[Setting[_]] = baseSettings ++ runSettings

  lazy val baseSettings: Seq[Setting[_]] = Seq (
    lwjgl.includePlatform := true,

    lwjgl.org := "org.lwjgl.lwjgl",

    lwjgl.utilsName := "lwjgl_util",

    nativesDir <<= (target) (_ / "lwjgl-natives"),

    manifestNatives <<= lwjglNativesTask,
    manifestNatives <<= manifestNatives dependsOn update,

    libraryDependencies <++=
      (lwjgl.version, lwjgl.org, lwjgl.utilsName, lwjgl.os, lwjgl.includePlatform) { 
        (v, org, utils, os, isNew) => 
        val deps = Seq(org % "lwjgl" % v, org % utils % v)

        if (isNew) 
          deps ++ Seq(org % "lwjgl-platform" % v classifier "natives-" + (os._1))
        else deps
      }
  )

  lazy val runSettings: Seq[Setting[_]] = Seq (
    lwjgl.version := "2.8.1",

    lwjgl.nativesName := "lwjgl-platform",

    lwjgl.nativesJarName <<= (lwjgl.nativesName, lwjgl.version, lwjgl.os) {
      _ + "-" + _ + "-natives-" + _._1
    },

    lwjgl.os := defineOs,
    copyDir <<= (resourceManaged in Compile) (_ / "lwjgl-resources"),

    copyNatives <<= lwjglCopyTask,
    resourceGenerators in Compile <+= copyNatives,

    cleanFiles <+= copyDir,

    fork := true,
    javaOptions <+= (copyDir, lwjgl.os) { (dir, os) => 
      "-Djava.library.path=%s".format(dir / os._1)
    }
  )

  lazy val oldLwjglSettings: Seq[Setting[_]] = lwjglSettings ++ Seq (
    resolvers += "Diablo-D3" at "http://adterrasperaspera.com/lwjgl",

    lwjgl.nativesName := "lwjgl-native",

    lwjgl.nativesJarName <<= (lwjgl.nativesName, lwjgl.version) (_ + "-" + _),

    lwjgl.version := "2.7.1",

    lwjgl.org := "org.lwjgl",

    lwjgl.utilsName := "lwjgl-util",

    lwjgl.includePlatform := false
  )

}
