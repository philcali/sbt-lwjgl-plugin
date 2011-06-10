import sbt._

import java.net.URL
import java.util.regex.Pattern

/**
 * JMonkey, unfortunately, does not have a public maven repo
 * We are going to hack the dependency by pulling down their
 * nightly builds and extracting the dependecies we need.
 */
/*
trait JMonkey extends LWJGLProject {
  lazy val baseRepo = "http://jmonkeyengine.com/nightly" 
  lazy val jname = "%s_%s" format(jmonkeyBaseVersion, targetedVersion)
  
  // This is created for the developer
  lazy val jMonkey = "org.jmonkeyengine" % "jmonkeyengine" % jmd 
  lazy val joggd = "de.jogg" % "j-ogg-oggd" % "1.0"
  lazy val joggvorb = "de.jogg" % "j-ogg-vorbisd" % "1.0"

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
  } describedAs "Pulls jMonkey dependency from nightly build."

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
  } describedAs "Displays any Jmonkey libraries installed on your machine."

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
  } dependsOn jmonkeyUpdate describedAs "Installs the j-ogg jars to your ivy cache"

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
  } dependsOn joggCache describedAs "Installs jMonkey lib on local machine"

  lazy val jmonkeyCleanLib = task { jmCleanLib(); None } describedAs "Clears downloaded jMonkey in lib." 

  lazy val jmonkeyCleanCache = task {
    FileUtilities.clean(jmonkeyParentCacheDir, log)
    None
  } describedAs "Clears installed jMonkey libs"

  // Used in pseudo caching
  lazy val jme = jmonkeyBaseVersion.split("jME")(1)
  lazy val jmd = "%s.0_%s" format(jme, targetedVersion) 
  lazy val jmonkeyParentCacheDir =
    Path.fromString(Path.userHome, ".ivy2/local/org.jmonkeyengine/jmonkeyengine")
  lazy val jmonkeyCachDir = jmonkeyParentCacheDir / "%s".format(jmd) 

  // Giving the ability for users to override
  // the base version and targeted nightly build
  def jmonkeyBaseVersion = "jME3"
  def targetedVersion = dateString(today)

  // Plugins are compiled in scala 2.7.7...
  def today = new java.util.Date()
  def dateString(when: java.util.Date) = {
    val sdf = new java.text.SimpleDateFormat("yyyy-MM-dd")
    sdf.format(when)
  }

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
