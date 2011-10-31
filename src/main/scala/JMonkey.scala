import sbt._

import Keys._

import java.net.URL
import java.util.regex.Pattern

import Helpers._

import dispatch.{Http, url => dUrl}

/**
 * JMonkey, unfortunately, does not have a public maven repo
 * We are going to hack the dependency by pulling down their
 * nightly builds and extracting the dependecies we need.
 */
object JMonkeyProject extends Plugin {
  import jmonkey._

  val http = new Http

  object jmonkey {
    /** JMonkey Settings */
    val version = SettingKey[String]("jmonkey-version")

    val baseRepo = SettingKey[String]("jmonkey-repo", "jMonkey repo")

    val baseVersion = SettingKey[String]("jmonkey-base-version", 
      "jMonkey Base Version (jME2 | jME3)")

    val targetVersion = SettingKey[String]("jmonkey-target-version", 
      "Targeted jMonkey version (yyyy-mm-dd)")

    val targetDate = SettingKey[java.util.Date]("jmonkey-target-date",
      "jMonkey nightly is versioned by a timestamp, use those as well")

    val downloadDir = SettingKey[File]("jmonkey-download-directory",
      "jMonkey builds will be temporarily stored here.")

    val targetPlatform = SettingKey[String]("jmonkey-target-platform",
      "Targeted platform (desktop | android)")

    val userAgent = SettingKey[String]("jmonkey-downloader-user-agent",
      "Use this user agent when downloading from nightly.")

    /** JMonkey Tasks */
    val download = TaskKey[Unit]("jmonkey-download", 
      "Pulls jMonkey dependency from specified repo.") 

    val install = TaskKey[Unit]("jmonkey-install",
      "Installs jMonkey lib on local machine")

    val listInstalled = TaskKey[Unit]("jmonkey-list-installed",
      "Displays any Jmonkey libraries installed on your machine.")

    val cleanLib = TaskKey[Unit]("jmonkey-clean-lib",
      "Purges the jMonkey install in the cache.")

    val cleanCache = TaskKey[Unit]("jmonkey-clean-cache",
      "Purges the jMonkey installs in the local cache.")
  }

  private def jmonkeyUpdateTask = 
    (streams, baseVersion, targetVersion, downloadDir, 
     baseRepo, version, userAgent, ivyPaths) map { 
      (s, bv, tv, dd, baseRepo, jmonkeyName, userAgent, ivyPaths) =>

      val cacheDir = jmonkeyCacheDir("desktop", bv, tv, ivyPaths.ivyHome)

      // First check that we don't have cached version
      (cacheDir.exists || (dd / jmonkeyName exists)) match {
        case true => 
          s.log.info("Already have %s" format(jmonkeyName))
        case false =>
          // If they wanted a nightly build then this could get extreme
          s.log.info("Cleaning older versions of %s" format(bv))
          val previousVersions = dd * "%s*".format(bv) 
          IO.delete(previousVersions.get)

          val zip = "%s.zip" format(jmonkeyName) 
          val zipFile = new java.io.File(zip)

          val jmUrl = dUrl(baseRepo) / zip

          val zipStream = new java.io.FileOutputStream(zipFile)
 
          // Start the download
          s.log.info("Downloading %s ..." format(jmonkeyName))
          s.log.warn("This may take a few minutes...")

          http((jmUrl <:< Map("User-Agent" -> userAgent)) >>> zipStream)
          
          // Extract the lib dir only...
          val dest = dd / jmonkeyName 
          val filter = new PatternFilter(Pattern.compile(".*jar"))
          IO.unzip(zipFile, dest, filter)
          // Destroy the zip
          zipFile.delete
          s.log.info("Complete")
      } 
  }

  private def jmonkeyCacheTask = 
    (streams, version, baseVersion, targetVersion, downloadDir, ivyPaths) map { 
      (s, jname, bv, tv, dd, ivyPaths) =>
      jmonkeyCacheDir("desktop", bv, tv, ivyPaths.ivyHome).exists match {
        case false =>
          // Need this for ivy
          val org = "org.jmonkeyengine"
          val revision = jmd(bv, tv)

          // yyyymmdd000000
          val pub = tv.split("-").mkString + "000000"

          // Get the jmonkey libs except those...
          val base = dd / jname
          val baselib = base / "lib"
          val exclude = baselib * "*test*" +++ (baselib * "lwjgl.jar") +++ (baselib * "*examples*")

          // jMonkey libs we're interested in 
          val interest = "jMonkeyEngine%s".format(jme(bv))
          val common = baselib * "*.jar" --- (exclude) +++ (base / "opt" ** "%s-bullet*".format(bv))
      
          // Different jMonkey jars for platform 
          val desktop = base * "%s.jar".format(interest)
          val android = base / "opt" ** "%s.jar".format(interest)

          val platforms = List("desktop", "android")

          platforms foreach { platform =>
            // Attempt to make the cache
            val cacheDir = jmonkeyCacheDir(platform, bv, tv, ivyPaths.ivyHome)
            val ivys = cacheDir / "ivys"
            val poms = cacheDir / "poms"

            // Build parents first
            s.log.info("Installing %s" format(jname))
            val children = common.get.map (_.base) ++ Seq("%s-%s".format(interest, platform))
            val module = "jmonkeyengine-%s".format(platform)

            val parentPom = pomMe(org, module, revision, children)
            val parentIvy = ivyParent(org, module, revision, children, pub)

            IO.write(ivys / "ivy.xml" asFile, parentIvy)
            IO.write((poms / "%s.pom".format(module)) asFile, parentPom)
          }

          val cacheBase = jmonkeyParentBaseDir(ivyPaths.ivyHome)

          // Write children next 
          common.get foreach { f =>
            val childCache = cacheBase / f.base / revision
            val childIvys = childCache / "ivys"
            val childJars = childCache / "jars"
            
            val civy = ivyMe(org, f.base, revision, f.base, pub)
            IO.write(childIvys / "ivy.xml" asFile, civy)
            IO.copyFile(f, childJars / f.name)
          }

          // Write these individually
          val handler = (platform: String) => (f: File) => {
            val newName = "%s-%s".format(f.base, platform)
            val childCache = cacheBase / newName / revision
            
            val civy = ivyMe(org, newName, revision, newName, pub)
            IO.write(childCache / "ivy" / "ivy.xml" asFile, civy)
            IO.copyFile(f, childCache / "jars" / (newName + ".jar"))
          }

          desktop.get foreach handler("desktop")
          android.get foreach handler("android")

          IO.delete(dd)
        case true => s.log.info("Already installed")
        s.log.info("Complete")
    }
  }

  private def jmonkeyLocalTask = (streams, targetPlatform, ivyPaths) map { (s, tp, ivys) =>
    s.log.info("Looking for jMonkey builds...")

    val targeted = "jmonkeyengine-%s".format(tp)
    val base = jmonkeyParentBaseDir(ivys.ivyHome)

    base / targeted exists match {
      case true => (base / targeted * "*").get.foreach { 
        f => s.log.info("Found: %s" format(f))
      }
      case false => 
        s.log.info("There are no builds in: %s" format(base))
    }
  }

  // Used in pseudo caching
  private def jme(baseVersion: String) = baseVersion.split("jME")(1)

  private def jmd(bv: String, tv: String) = 
    "%s.0_%s".format(jme(bv), tv) 

  private def jmonkeyCacheDir(platform: String, bv: String, tv: String, ivyHome: Option[File]) = { 
    val base = jmonkeyParentBaseDir(ivyHome)
    (base / "jmonkeyengine-%s".format(platform) / "%s".format(jmd(bv, tv))) 
  }

  private def jmonkeyParentBaseDir(ivyHome: Option[File]) =
    ivyHome.getOrElse (Path.userHome / ".ivy2") / "local" / "org.jmonkeyengine"

  private def createIfNotExists(d: File) = 
    if(!d.exists) IO.createDirectory(d)

  lazy val baseSettings: Seq[Setting[_]] = Seq (
      baseRepo := "http://jmonkeyengine.com/nightly",
      baseVersion := "jME3",
      targetDate := new java.util.Date(),
      targetVersion <<= (targetDate) {
        val sdf = new java.text.SimpleDateFormat("yyyy-MM-dd")
        sdf.format(_)
      },

      userAgent <<= (version, LWJGLPlugin.lwjgl.os) { (v, os) => 
        val operating = os match {
          case ("macosx", _) => "Macintosh"
          case ("windows", _) => "Windows"
          case ("linux", _) => "Linux"
          case ("solaris", _) => "Sun Solaris"
          case _ => "Unknown"
        } 
        "Mozilla/5.0 (compatible; jMonkeyDownloader %s; %s)" format(v, operating)
      },

      jmonkey.version <<= (baseVersion, targetVersion) { "%s_%s".format(_, _) },

      downloadDir := file(".") / "jmonkeyDownloads",
      targetPlatform := "desktop",

      download <<= jmonkeyUpdateTask,
      listInstalled <<= jmonkeyLocalTask,
      install <<= jmonkeyCacheTask,
      install <<= install dependsOn download, 

      cleanLib <<= (downloadDir) map { IO.delete(_) },

      cleanCache <<= (streams, ivyPaths) map { (s, ivys) => 
        s.log.info("Clearing out %s" format(jmonkeyParentBaseDir(ivys.ivyHome)))
        IO.delete(jmonkeyParentBaseDir(ivys.ivyHome))
      },

      update <<= update dependsOn install, 

      cleanFiles <+= downloadDir,

      // Create these dependecies for you 
      libraryDependencies <++= (targetPlatform, baseVersion, targetVersion) { 
        (platform, bv, tv) => Seq ( 
          "org.jmonkeyengine" % "jmonkeyengine-%s".format(platform) % jmd(bv, tv) 
        ) 
      }
  )

  lazy val jmonkeySettings: Seq[Setting[_]] =
    LWJGLPlugin.lwjglSettings ++ baseSettings
}
