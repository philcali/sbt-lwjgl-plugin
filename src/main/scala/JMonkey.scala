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
  // All the configurable settings
  val jmonkeyBaseRepo = SettingKey[String]("jmonkey-repo", "jMonkey repo")
  val jmonkeyVersion = SettingKey[String]("jmonkey-version", 
                                          "Targeted jMonkey version")
  val jmonkeyBase = SettingKey[String]("jmonkey-base-version", 
                                       "jMonkey Base Version")
  val jmonkeyVersionDate = SettingKey[java.util.Date]("jmonkey-verion-date",
               "jMonkey nightly is versioned by a timestamp, so we use those as well")

  // All the configurable tasks
  lazy val jmonkeyUpdate = TaskKey[Unit]("jmonkey-update", 
                                  "Pulls jMonkey dependency from specified repo.") 
  private def jmonkeyUpdateTask = (streams) map { s =>
  }
  lazy val jmonkeyCache = TaskKey[Unit]("jmonkey-cache",
                                  "Installs jMonkey lib on local machine")
  private def jmonkeyCacheTask = (streams) map { s =>
  }
  // TODO: maybe revisit this one
  lazy val jmonkeyLocal = TaskKey[Unit]("jmonkey-local",
                      "Displays any Jmonkey libraries installed on your machine.")
  private def jmonkeyLocalTask = (streams) map { s =>
  }
  // TODO: maybe revisit this one too
  lazy val jmonkeyCleanLib = TaskKey[Unit]("jmonkey-clean-lib",
                      "Purges the jMonkey install in the cache.")
  private def jmonkeyCleanLibTask = (streams) map { s =>
  }
  lazy val jmonkeyCleanCache = TaskKey[Unit]("jmonkey-clean-cache",
                      "Purges the jMonkey installs in the local cache.")
  private def jmonkeyCleanCacheTask = (streams) map { s =>
  }
  lazy val joggCache = TaskKey[Unit]("jogg-cache",
                      "Installs the j-ogg jars to your ivy cache")
  private def joggCacheTask = (streams) map { s =>
  }

  // Used in pseudo caching
  private def jme(baseVersion: String) = baseVersion.split("jME")(1)
  private def jmd(bv: String, targetedVersion: String) = 
    "%s.0_%s".format(jme(bv), targetedVersion) 
  private def jmonkeyCachDir(bv: String, tv: String) = 
    jmonkeyParentCacheDir / "%s".format(jmd(bv, tv)) 
  lazy val jmonkeyParentCacheDir =
    Path.userHome / ".ivy2" / "local" / "org.jmonkeyengine" / "jmonkeyengine"

  lazy val engineSettings = Seq (
    // Configurable settings
    jmonkeyBaseRepo := "http://jmonkeyengine.com/nightly",
    jmonkeyBase := "jME3",
    jmonkeyVersionDate := new java.util.Date(),
    jmonkeyVersion <<= (jmonkeyVersionDate) {
      val sdf = new java.text.SimpleDateFormat("yyyy-MM-dd")
      sdf.format(_)
    },
    
    // Configurable tasks
    jmonkeyUpdate <<= jmonkeyUpdateTask,

    // We create these dependecies for you 
    libraryDependencies <++= (jmonkeyBase, jmonkeyVersion) { (bv, tv) => Seq ( 
      "org.jmonkeyengine" % "jmonkeyengine" % jmd(bv, tv), 
      "de.jogg" % "j-ogg-oggd" % "1.0",
      "de.jogg" % "j-ogg-vorbisd" % "1.0"
    ) }
  )
}
/*
trait JMonkey extends LWJGLProject {
  lazy val jname = "%s_%s" format(jmonkeyBaseVersion, targetedVersion)
  
  // Bulk of the work, any exception here can
  // bubble up to the updateAction
  lazy val jmonkeyUpdate = task {
    // First check that we don't have cached version
    (jmonkeyCachDir.exists || (dependencyPath / jname).exists) match {
      case true => 
        log.info("Already have %s" format(jname))
        None
      case false =>
        // If they wanted a nightly build then this could get extreme
        log.info("Cleaning older versions of %s" format(jmonkeyBaseVersion))
        val previousVersions = dependencyPath * "%s*".format(jmonkeyBaseVersion) 
        FileUtilities.clean(previousVersions.get, log)

        val zip = "%s.zip" format(jname) 
        val zipFile = new java.io.File(zip)

        val url = new URL("%s/%s" format(baseRepo, zip))
        // Start the download
        log.info("Downloading %s ..." format(jname))
        log.warn("This may take a few minutes...")
        FileUtilities.download(url, zipFile, log) 
        
        // Extract the lib dir only...
        val dest = dependencyPath / jname
        val filter = new PatternFilter(Pattern.compile(".*jar"))
        FileUtilities.unzip(zipFile, dest, filter, log)
        // Destroy the zip
        zipFile.delete
        log.info("Complete")
        None
    } 
  } 

  // Tries to find any jmonkey libs in the cache
  lazy val jmonkeyLocal = task {
    log.info("Looking for jMonkey builds: %s" format(jmd))
    jmonkeyParentCacheDir.exists match {
      case true => (jmonkeyParentCacheDir * "*").get.foreach { 
        f => log.info("Found: %s" format(f.base))
      }
      case false => log.info("There are no builds in: %s" format(jmonkeyParentCacheDir))
    }
    None
  } describedAs 

  lazy val joggCache = task {
    val joggOrg = "de.jogg"
    val basePath = Path.fromString(Path.userHome, ".ivy2/local/%s".format(joggOrg))

    createIfNotExists (basePath)    

    val depJars = dependencyPath / jname / "lib" * "j-ogg*"

    depJars.get foreach { jar =>
      val module = jar.base
      val cachePath = basePath / module / "1.0"
      val ivys = cachePath / "ivys"
      val jars = cachePath / "jars"
      
      List(cachePath, ivys, jars) foreach createIfNotExists
      FileUtilities.copyFlat(List(jar), jars, log)
      val ivyXml = ivyMe(joggOrg, module, "1.0", module)
      val ivyXmlFile = ivys / "ivy.xml"
      FileUtilities.write(ivyXmlFile.asFile, ivyContents(ivyXml.toString), log)  
    }
    None
  } dependsOn jmonkeyUpdate describedAs 

  lazy val jmonkeyCache = task {
    // Attempt to make the cache
    val ivys = jmonkeyCachDir / "ivys"
    val jars = jmonkeyCachDir / "jars"

    List(jmonkeyCachDir, ivys, jars) foreach createIfNotExists 

    // jMonkey lib we're interested 
    val interest = "jMonkeyEngine%s".format(jme)
    val jlibs = dependencyPath / jname * "%s.jar".format(interest)
   
    log.info("Installing %s" format(jname))
    FileUtilities.copyFlat(jlibs.get, jars, log)
    
    val jmonkeyIvy = ivyMe("org.jmonkeyengine", "jmonkeyengine", jmd, interest)
    val ivyLocation = ivys / "ivy.xml"
    FileUtilities.write(ivyLocation.asFile, ivyContents(jmonkeyIvy.toString), log)
    log.info("Complete")
    jmCleanLib()
    None
  } dependsOn joggCache describedAs 

  lazy val jmonkeyCleanLib = task { jmCleanLib(); None } describedAs "Clears downloaded jMonkey in lib." 

  lazy val jmonkeyCleanCache = task {
    FileUtilities.clean(jmonkeyParentCacheDir, log)
    None
  } describedAs "Clears installed jMonkey libs"


  def jmCleanLib() {
    val lib = dependencyPath / jname
    FileUtilities.clean(lib, log)
    lib.asFile.delete
  }

  def ivyContents(xml: String) = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + xml

  def ivyMe(org: String, module: String, revision: String, artifact: String) = {
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
  
  def createIfNotExists(d: Path) = 
    if(!d.exists) FileUtilities.createDirectory(d, log)

  override def updateAction = 
    super.updateAction dependsOn jmonkeyCache
}
*/
