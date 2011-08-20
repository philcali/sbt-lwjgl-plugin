import sbt._

import Keys._
import LWJGLKeys._

import java.net.URL
import java.util.regex.Pattern

import Helpers._

/**
 * JMonkey, unfortunately, does not have a public maven repo
 * We are going to hack the dependency by pulling down their
 * nightly builds and extracting the dependecies we need.
 */
object JMonkeyProject {

  private def jmonkeyUpdateTask = 
    (streams, baseVersion, targetVersion, downloadDir, 
     baseRepo, version) map { 
      (s, bv, tv, dd, baseRepo, jmonkeyName) =>
      val cacheDir = jmonkeyCacheDir("desktop", bv, tv)
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

          val url = new URL("%s/%s" format(baseRepo, zip))
          // Start the download
          s.log.info("Downloading %s ..." format(jmonkeyName))
          s.log.warn("This may take a few minutes...")
          IO.download(url, zipFile)
          
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
    (streams, version, baseVersion, targetVersion, downloadDir) map { 
      (s, jname, bv, tv, dd) =>
      jmonkeyCacheDir("desktop", bv, tv).exists match {
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
          val common = baselib * "*.jar" --- (exclude) +++ (base / "opt" ** "*natives.jar")
      
          // Different jMonkey jars for platform 
          val desktop = base * "%s.jar".format(interest)
          val android = base / "opt" ** "%s.jar".format(interest)

          val platforms = List("desktop", "android")

          platforms foreach { platform =>
            // Attempt to make the cache
            val cacheDir = jmonkeyCacheDir(platform, bv, tv)
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

          // Write children next 
          common.get foreach { f =>
            val childCache = jmonkeyParentBaseDir / f.base / revision
            val childIvys = childCache / "ivys"
            val childJars = childCache / "jars"
            
            val civy = ivyMe(org, f.base, revision, f.base, pub)
            IO.write(childIvys / "ivy.xml" asFile, civy)
            IO.copyFile(f, childJars / f.name)
          }

          // Write these individually
          val handler = (platform: String) => (f: File) => {
            val newName = "%s-%s".format(f.base, platform)
            val childCache = jmonkeyParentBaseDir / newName / revision
            
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

  private def jmonkeyLocalTask = (streams) map { s =>
    s.log.info("Looking for jMonkey builds...")
    val targeted = "jmonkeyengine-desktop"
    jmonkeyParentBaseDir / targeted exists match {
      case true => (jmonkeyParentBaseDir / targeted * "*").get.foreach { 
        f => s.log.info("Found: %s" format(f))
      }
      case false => 
        s.log.info("There are no builds in: %s" format(jmonkeyParentBaseDir))
    }
  }

  // Used in pseudo caching
  private def jme(baseVersion: String) = baseVersion.split("jME")(1)

  private def jmd(bv: String, tv: String) = 
    "%s.0_%s".format(jme(bv), tv) 

  private def jmonkeyCacheDir(platform: String, bv: String, tv: String) = 
    jmonkeyParentBaseDir / "jmonkeyengine-%s".format(platform) / "%s".format(jmd(bv, tv)) 

  lazy val jmonkeyParentBaseDir =
   Path.userHome / ".ivy2" / "local" / "org.jmonkeyengine"


  private def createIfNotExists(d: File) = 
    if(!d.exists) IO.createDirectory(d)

  lazy val engineSettings: Seq[Setting[_]] = LWJGLProject.engineSettings ++ 
    inConfig(JMonkey) { Seq (
      // Configurable settings
      baseRepo := "http://jmonkeyengine.com/nightly",
      baseVersion := "jME3",
      targetDate := new java.util.Date(),
      targetVersion <<= (targetDate) {
        val sdf = new java.text.SimpleDateFormat("yyyy-MM-dd")
        sdf.format(_)
      },

      version <<= (baseVersion, targetVersion) { "%s_%s".format(_, _) },

      downloadDir := file(".") / "jmonkeyDownloads",
      targetPlatform := "desktop",
      
      // Configurable tasks
      download <<= jmonkeyUpdateTask,
      listInstalled <<= jmonkeyLocalTask,
      install <<= jmonkeyCacheTask,
      install <<= install dependsOn download, 

      cleanLib <<= (downloadDir) map { IO.delete(_) },

      cleanCache <<= (streams) map { s => 
        s.log.info("Clearing out %s" format(jmonkeyParentBaseDir))
        IO.delete(jmonkeyParentBaseDir)
      }

    ) } ++ Seq(
      update <<= update dependsOn (install in JMonkey),

      cleanFiles <+= (downloadDir in JMonkey).identity,

      // Create these dependecies for you 
      libraryDependencies <++= (targetPlatform in JMonkey, baseVersion in JMonkey, targetVersion in JMonkey) { 
        (platform, bv, tv) => Seq ( 
          "org.jmonkeyengine" % "jmonkeyengine-%s".format(platform) % jmd(bv, tv) 
        ) 
      }
    )
}
