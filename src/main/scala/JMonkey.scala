import sbt._

import Keys._

import java.net.URL
import java.util.regex.Pattern

/**
 * JMonkey, unfortunately, does not have a public maven repo
 * We are going to hack the dependency by pulling down their
 * nightly builds and extracting the dependecies we need.
 */
object JMonkey extends Plugin {
  val JMonkey = config("jmonkey")

  // All the configurable settings
  val jmonkeyBaseRepo = SettingKey[String]("jmonkey-repo", "jMonkey repo")
  val jmonkeyVersion = SettingKey[String]("jmonkey-version", 
                        "The complete jMonkey version")
  val jmonkeyBase = SettingKey[String]("jmonkey-base-version", 
                                       "jMonkey Base Version (jME2 | jME3)")
  val jmonkeyTargeted = SettingKey[String]("jmonkey-version", 
                                       "Targeted jMonkey version (2011-04-22)")
  val jmonkeyTargetedDate = SettingKey[java.util.Date]("jmonkey-verion-date",
               "jMonkey nightly is versioned by a timestamp, use those as well")
  val jmonkeyDownloadDir = SettingKey[File]("jmonkey-download-directory",
               "jMonkey builds will be temporarily stored here.")

  // All the configurable tasks
  lazy val jmonkeyUpdate = TaskKey[Unit]("jmonkey-update", 
                                  "Pulls jMonkey dependency from specified repo.") 
  private def jmonkeyUpdateTask = 
    (streams, jmonkeyBase, jmonkeyTargeted, jmonkeyDownloadDir, 
     jmonkeyBaseRepo, jmonkeyVersion) map { 
      (s, bv, tv, dd, baseRepo, jmonkeyName) =>
      val cacheDir = jmonkeyCachDir(bv, tv)
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

  lazy val jmonkeyCache = TaskKey[Unit]("jmonkey-cache",
                                  "Installs jMonkey lib on local machine")
  private def jmonkeyCacheTask = 
    (streams, jmonkeyVersion, jmonkeyBase, jmonkeyTargeted, jmonkeyDownloadDir) map { 
      (s, jname, bv, tv, dd) =>
      val cacheDir = jmonkeyCachDir(bv, tv)
      // Attempt to make the cache
      val ivys = cacheDir / "ivys"
      val jars = cacheDir / "jars"

      List(cacheDir, ivys, jars) foreach createIfNotExists 

      // jMonkey lib we're interested 
      val interest = "jMonkeyEngine%s".format(jme(bv))
      val jlibs = dd / jname * "%s.jar".format(interest)
     
      s.log.info("Installing %s" format(jname))
      jlibs.get foreach (IO.copyFile(_, jars))
      
      val jmonkeyIvy = ivyMe("org.jmonkeyengine", "jmonkeyengine", jmd(bv, tv), interest)
      val ivyLocation = ivys / "ivy.xml"
      IO.write(ivyLocation.asFile, ivyContents(jmonkeyIvy.toString))
      s.log.info("Complete")
      IO.delete(dd)
  }
  lazy val jmonkeyLocal = TaskKey[Unit]("jmonkey-local",
                      "Displays any Jmonkey libraries installed on your machine.")
  private def jmonkeyLocalTask = (streams) map { s =>
    s.log.info("Looking for jMonkey builds...")
    jmonkeyParentCacheDir.exists match {
      case true => (jmonkeyParentCacheDir * "*").get.foreach { 
        f => s.log.info("Found: %s" format(f.base))
      }
      case false => 
        s.log.info("There are no builds in: %s" format(jmonkeyParentCacheDir))
    }
  }
  // TODO: maybe revisit this one
  lazy val jmonkeyCleanLib = TaskKey[Unit]("jmonkey-clean-lib",
                      "Purges the jMonkey install in the cache.")
  // TODO: maybe revisit this one too
  lazy val jmonkeyCleanCache = TaskKey[Unit]("jmonkey-clean-cache",
                      "Purges the jMonkey installs in the local cache.")

  lazy val joggCache = TaskKey[Unit]("jogg-cache",
                      "Installs the j-ogg jars to your ivy cache")
  private def joggCacheTask = 
    (streams, jmonkeyVersion, jmonkeyDownloadDir) map { 
      (s, jmonkeyName, dd) =>
      val joggOrg = "de.jogg"
      val basePath = Path.userHome / ".ivy2" / "local" / "%s".format(joggOrg)

      createIfNotExists (basePath)

      val depJars = dd / jmonkeyName / "lib" * "j-ogg*"

      depJars.get foreach { jar =>
        val module = jar.base
        val cachePath = basePath / module / "1.0"
        val ivys = cachePath / "ivys"
        val jars = cachePath / "jars"
        
        List(cachePath, ivys, jars) foreach createIfNotExists
        IO.copyFile(jar, jars)
        val ivyXml = ivyMe(joggOrg, module, "1.0", module)
        val ivyXmlFile = ivys / "ivy.xml"
        IO.write(ivyXmlFile.asFile, ivyContents(ivyXml.toString))
      }
  }

  // Used in pseudo caching
  private def jme(baseVersion: String) = baseVersion.split("jME")(1)

  private def jmd(bv: String, tv: String) = 
    "%s.0_%s".format(jme(bv), tv) 

  private def jmonkeyCachDir(bv: String, tv: String) = 
    jmonkeyParentCacheDir / "%s".format(jmd(bv, tv)) 

  lazy val jmonkeyParentCacheDir =
    Path.userHome / ".ivy2" / "local" / "org.jmonkeyengine" / "jmonkeyengine"

  private def ivyContents(xml: String) = 
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + xml

  private def ivyMe(org: String, module: String, revision: String, artifact: String) = {
<ivy-module version="1.0" xmlns:e="http://ant.apache.org/ivy/extra">
  <info organisation={org} module={module} revision={revision} status="release" publication={new java.util.Date().getTime.toString}/>
  <configurations>
    <conf name="compile" visibility="public" description=""/>
    <conf name="runtime" visibility="public" description=""/>
    <conf name="provided" visibility="public" description=""/>
    <conf name="system" visibility="public" description=""/>
    <conf name="optional" visibility="public" description=""/>
    <conf name="sources" visibility="public" description=""/>
    <conf name="javadoc" visibility="public" description=""/>
  </configurations>
  <publications>
    <artifact name={artifact} type="jar" ext="jar" conf="compile,runtime,provided,system,optional,sources,javadoc"/>
  </publications>
</ivy-module>
  }

  private def createIfNotExists(d: File) = 
    if(!d.exists) IO.createDirectory(d)

  lazy val engineSettings = Seq (
    // Configurable settings
    jmonkeyBaseRepo := "http://jmonkeyengine.com/nightly",
    jmonkeyBase := "jME3",
    jmonkeyTargetedDate := new java.util.Date(),
    jmonkeyTargeted <<= (jmonkeyTargetedDate) {
      val sdf = new java.text.SimpleDateFormat("yyyy-MM-dd")
      sdf.format(_)
    },
    jmonkeyVersion <<= (jmonkeyBase, jmonkeyTargeted) { "%s_%s".format(_, _) },
    jmonkeyDownloadDir <<= (target) { _ / "jmonkeyDownloads" },
    
    // Configurable tasks
    jmonkeyUpdate <<= jmonkeyUpdateTask,
    jmonkeyLocal <<= jmonkeyLocalTask,
    joggCache in JMonkey <<= joggCacheTask,
    joggCache <<= Seq(jmonkeyUpdate, joggCache in JMonkey).dependOn,
    jmonkeyCache in JMonkey <<= jmonkeyCacheTask,
    jmonkeyCache <<= Seq(jmonkeyUpdate, joggCache, jmonkeyCache in JMonkey).dependOn,

    update <<= update dependsOn jmonkeyCache,

    jmonkeyCleanLib <<= (jmonkeyDownloadDir) map { IO.delete(_) },
    jmonkeyCleanCache <<= (streams) map { _ => IO.delete(jmonkeyParentCacheDir) },

    // We create these dependecies for you 
    libraryDependencies <++= (jmonkeyBase, jmonkeyVersion) { (bv, tv) => Seq ( 
      "org.jmonkeyengine" % "jmonkeyengine" % jmd(bv, tv), 
      "de.jogg" % "j-ogg-oggd" % "1.0",
      "de.jogg" % "j-ogg-vorbisd" % "1.0"
    ) }
  )
}
