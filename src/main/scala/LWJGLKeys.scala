import sbt._

object LWJGLKeys {
  val LWJGL = config("lwjgl") extend Compile

  val Slick = config("slick")

  val Nicol = config("nicol")

  val JMonkey = config("jmonkey")

  val Ardor = config("ardor")

  /** LWJGL Settings */
  val copyDir = SettingKey[File]("copy-location", 
    "This is where lwjgl resources will be copied")

  val nativesDir = SettingKey[File]("natives-directory", 
    "This is the location where the lwjgl-natives will bomb to") 

  val os = SettingKey[(String, String)]("os", 
    "This is the targeted OS for the build. Defaults to the running OS.")

  /** JMonkey Settings */
  val baseRepo = SettingKey[String]("repo", "jMonkey repo")

  val baseVersion = SettingKey[String]("base-version", 
    "jMonkey Base Version (jME2 | jME3)")

  val targetVersion = SettingKey[String]("target-version", 
    "Targeted jMonkey version (2011-04-22)")

  val targetDate = SettingKey[java.util.Date]("target-date",
    "jMonkey nightly is versioned by a timestamp, use those as well")

  val downloadDir = SettingKey[File]("download-directory",
    "jMonkey builds will be temporarily stored here.")

  val targetPlatform = SettingKey[String]("target-platform",
    "Targeted platform (desktop | android)")

  /** LWJGL Tasks */ 
  val copyNatives = TaskKey[Seq[File]]("copy-natives", 
    "Copies the lwjgl library from natives jar to managed resources")

  val targetNatives = TaskKey[Unit]("natives", 
    "Copy LWJGL resources to output directory")

  /** Slick Task */
  val patch = TaskKey[Unit]("patch", 
    "The phys2d dependency pom is broken. Patch aims to fix it")

  /** JMonkey Tasks */
  val download = TaskKey[Unit]("download", 
    "Pulls jMonkey dependency from specified repo.") 

  val install = TaskKey[Unit]("install",
    "Installs jMonkey lib on local machine")

  val listInstalled = TaskKey[Unit]("list-installed",
    "Displays any Jmonkey libraries installed on your machine.")

  val cleanLib = TaskKey[Unit]("clean-lib",
    "Purges the jMonkey install in the cache.")

  val cleanCache = TaskKey[Unit]("clean-cache",
    "Purges the jMonkey installs in the local cache.")

}
