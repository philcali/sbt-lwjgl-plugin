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
  val jmonkeyTargeted = SettingKey[String]("jmonkey-target", 
                                       "Targeted jMonkey version (2011-04-22)")
  val jmonkeyTargetedDate = SettingKey[java.util.Date]("jmonkey-target-date",
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
      val cacheDir = jmonkeyCacheDir(bv, tv)
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
      val cacheDir = jmonkeyCacheDir(bv, tv)
      (cacheDir.exists) match {
        case false =>
          // Attempt to make the cache
          val ivys = cacheDir / "ivys"
          val poms = cacheDir / "poms"

          // Need this for ivy
          val org = "org.jmonkeyengine"
          val revision = jmd(bv, tv)

          // yyyymmdd000000
          val pub = tv.split("-").mkString + "000000"

          List(cacheDir, ivys, poms) foreach createIfNotExists 

          // Get the jmonkey libs except those...
          val base = dd / jname
          val baselib = base / "lib"
          val exclude = baselib * "*test*" +++ (baselib * "lwjgl.jar") +++ (baselib * "*examples*")

          // jMonkey libs we're interested in 
          val interest = "jMonkeyEngine%s".format(jme(bv))
          val jlibs = baselib * "*.jar" --- (exclude) +++ (base * "%s.jar".format(interest)) +++ (base / "opt" ** "*natives.jar")
        
          // Build parent first
          s.log.info("Installing %s" format(jname))
          val children = jlibs.get map (_.base)

          val parentPom = xmlContents(pomMe(org, "jmonkeyengine", revision, children).toString)
          val parentIvy = xmlContents(ivyParent(org, "jmonkeyengine", revision, children, pub).toString)
          IO.write(ivys / "ivy.xml" asFile, parentIvy)
          IO.write(poms / "jmonkeyengine.pom" asFile, parentPom)
        
          // Write children next 
          jlibs.get foreach { f =>
            val childCache = jmonkeyParentBaseDir / f.base / revision
            val childIvys = childCache / "ivys"
            val childJars = childCache / "jars"
            
            val civy = xmlContents(ivyMe(org, f.base, revision, f.base, pub).toString)
            IO.write(childIvys / "ivy.xml" asFile, civy)
            IO.copyFile(f, childJars / f.name)
          }
          IO.delete(dd)
        case true => s.log.info("Already installed")
        s.log.info("Complete")
    }
  }
  lazy val jmonkeyLocal = TaskKey[Unit]("jmonkey-local",
                      "Displays any Jmonkey libraries installed on your machine.")
  private def jmonkeyLocalTask = (streams) map { s =>
    s.log.info("Looking for jMonkey builds...")
    jmonkeyParentCacheDir.exists match {
      case true => (jmonkeyParentCacheDir * "*").get.foreach { 
        f => s.log.info("Found: %s" format(f))
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

  // Used in pseudo caching
  private def jme(baseVersion: String) = baseVersion.split("jME")(1)

  private def jmd(bv: String, tv: String) = 
    "%s.0_%s".format(jme(bv), tv) 

  private def jmonkeyCacheDir(bv: String, tv: String) = 
    jmonkeyParentCacheDir / "%s".format(jmd(bv, tv)) 

  lazy val jmonkeyParentBaseDir =
    Path.userHome / ".ivy2" / "local" / "org.jmonkeyengine"

  lazy val jmonkeyParentCacheDir = jmonkeyParentBaseDir / "jmonkeyengine"

  private def xmlContents(xml: String) = 
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + xml

  private def ivyParent(org: String, module: String, revision: String, children: Seq[String], pub: String) = {
<ivy-module version="2.0" xmlns:e="http://ant.apache.org/ivy/extra">
  <info organisation={org} module={module} revision={revision} status="release" publication={pub}/>
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
    <artifact name={module} type="pom" ext="pom" conf="compile,runtime,provided,system,optional,sources,javadoc"/>
  </publications>
  <dependencies>
    { children.map { child =>
      <dependency org={org} name={child} rev={revision} conf={scala.xml.Unparsed("compile->default(compile)")}>
      </dependency>
    }}
  </dependencies>
</ivy-module>   
  }

  private def pomMe(org: String, artifact: String, revision: String, children: Seq[String]) = {
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" 
         xmlns="http://maven.apache.org/POM/4.0.0" 
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <modelVersion>4.0.0</modelVersion>
    <groupId>{org}</groupId>
    <artifactId>{artifact}</artifactId>
    <packaging>pom</packaging>
    <version>{revision}</version>
    <dependencies>
        { children.map { child =>
          <dependency>
            <groupId>{org}</groupId>
            <artifactId>{child}</artifactId>
            <revision>{revision}</revision>
            <scope>compile</scope>
          </dependency>
        }}
    </dependencies>
    <repositories>
        <repository>
            <id>ScalaToolsMaven2Repository</id>
            <name>Scala-Tools Maven2 Repository</name>
            <url>http://scala-tools.org/repo-releases/</url>
        </repository>
    </repositories>
</project>
  }

  private def ivyMe(org: String, module: String, revision: String, artifact: String, pub: String) = {
<ivy-module version="1.0" xmlns:e="http://ant.apache.org/ivy/extra">
  <info organisation={org} module={module} revision={revision} status="release" publication={pub}/>
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
    jmonkeyDownloadDir := file(".") / "jmonkeyDownloads",
    
    // Configurable tasks
    jmonkeyUpdate in JMonkey <<= jmonkeyUpdateTask,
    jmonkeyLocal in JMonkey <<= jmonkeyLocalTask,
    jmonkeyCache in JMonkey <<= jmonkeyCacheTask,
    jmonkeyCache in JMonkey <<= jmonkeyCache in JMonkey dependsOn (jmonkeyUpdate in JMonkey),

    update <<= update dependsOn (jmonkeyCache in JMonkey),

    jmonkeyCleanLib in JMonkey <<= (jmonkeyDownloadDir) map { IO.delete(_) },
    jmonkeyCleanCache in JMonkey <<= (streams) map { s => 
      s.log.info("Clearing out %s" format(jmonkeyParentBaseDir))
      IO.delete(jmonkeyParentBaseDir)
    },

    // We create these dependecies for you 
    libraryDependencies <++= (jmonkeyBase, jmonkeyTargeted) { (bv, tv) => Seq ( 
      "org.jmonkeyengine" % "jmonkeyengine" % jmd(bv, tv) 
    ) }
  )
}
