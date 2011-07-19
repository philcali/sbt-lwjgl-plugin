import sbt._

object LWJGLKeys {
  /** LWJGL Settings */
  val lwjglCopyDir = SettingKey[File]("lwjgl-copy-location", 
    "This is where lwjgl resources will be copied")

  val lwjglNativesDir = SettingKey[File]("lwjgl-natives-directory", 
    "This is the location where the lwjgl-natives will bomb to") 

  val lwjglVersion = SettingKey[String]("lwjgl-version", 
    "This is the targeted LWJGL verision")

  val lwjglOs = SettingKey[(String, String)]("lwjgl-os", 
    "This is the targeted OS for the build. Defaults to the running OS.")

  /** Slick Setting */
  val slickVersion = SettingKey[String]("slick-version", 
    "The version of Slick2D in the Maven Repo")

  /** Nicol Setting */
  val nicolVersion = SettingKey[String]("nicol-version", 
    "The version of Nicol in the Maven repo")

  /** JMonkey Settings */
  val jmonkeyBaseRepo = SettingKey[String]("jmonkey-repo", "jMonkey repo")

  val jmonkeyVersion = SettingKey[String]("jmonkey-version", 
    "The complete jMonkey version")
  val jmonkeyBase = SettingKey[String]("jmonkey-base-version", 
    "jMonkey Base Version (jME2 | jME3)")
  val jmonkeyTargeted = SettingKey[String]("jmonkey-target", 
    "Targeted jMonkey version (2011-04-22)")
  val jmonkeyTargetedDate = SettingKey[java.util.Date]("jmonkey-target-date",
    "jMonkey nightly is versioned by a timestamp, use those as well")
  val jmonkeyDownloadDir = SettingKey[File]("jmonkey-download-directory",
    "jMonkey builds will be temporarily stored here.")
  val jmonkeyPlatform = SettingKey[String]("jmonkey-platform",
    "Targeted platform (desktop | android)")

  /** LWJGL Tasks */ 
  lazy val lwjglCopy = TaskKey[Seq[File]]("lwjgl-copy", 
    "Copies the lwjgl library from natives jar to managed resources")

  val lwjglClean = TaskKey[Unit]("lwjgl-clean", "Clean the LWJGL resource dir")

  val lwjglNatives = TaskKey[Unit]("lwjgl-natives", 
    "Copy LWJGL resources to output directory")

  /** Slick Task */
  val slickPatch = TaskKey[Unit]("slick-patch", 
    "The phys2d dependency pom is broken. Patch aims to fix it")

  /** JMonkey Tasks */
  lazy val jmonkeyUpdate = TaskKey[Unit]("jmonkey-update", 
    "Pulls jMonkey dependency from specified repo.") 

  lazy val jmonkeyCache = TaskKey[Unit]("jmonkey-cache",
    "Installs jMonkey lib on local machine")

  lazy val jmonkeyLocal = TaskKey[Unit]("jmonkey-local",
    "Displays any Jmonkey libraries installed on your machine.")

  lazy val jmonkeyCleanLib = TaskKey[Unit]("jmonkey-clean-lib",
    "Purges the jMonkey install in the cache.")

  lazy val jmonkeyCleanCache = TaskKey[Unit]("jmonkey-clean-cache",
    "Purges the jMonkey installs in the local cache.")

}
