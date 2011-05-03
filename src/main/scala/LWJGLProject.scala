import sbt._

import scala.io.Source
import java.net.URL
import java.util.regex.Pattern
import java.io.{ FileNotFoundException, FileOutputStream }

abstract class LWJGLProject(info: ProjectInfo) extends DefaultProject(info) { 
  lazy val compilePath = managedDependencyPath / "compile"
	
  lazy val lwjglRepo = "Diablo-D3" at "http://adterrasperaspera.com/lwjgl"

	lazy val lwjgl = "org.lwjgl" % "lwjgl" % lwjglVersion
	lazy val lwjglUtils = "org.lwjgl" % "lwjgl-util" % lwjglVersion
  lazy val compileJ = compilePath ** "*.jar"
 
	private lazy val defineOs = System.getProperty("os.name").toLowerCase.take(3).toString match {
		case "lin" => ("linux", ":", "so")
		case "mac" | "dar" => ("macosx", ":", "lib")
		case "win" => ("windows", ";", "dll")
		case "sun" => ("solaris", ":", "so")
		case _ => ("unknown", "", "")
	}

  // Extracts LWJGL native jar to this location
	private lazy val nativeLibPath = dependencyPath / lwjglJar

	lazy val copyLwjgl = task {
		try {
			log.info("Copying files for %s" format(defineOs._1))
			if(nativeLibPath.exists) {
				log.info("Skipping because of existence: %s" format(nativeLibPath))
			} else {
				val filter = new PatternFilter(Pattern.compile(defineOs._1 + "/.*" + defineOs._3))
				FileUtilities.unzip(compilePath / "%s.jar".format(lwjglJar), nativeLibPath, filter, log)
			}
			None
		} catch {
			case e: FileNotFoundException => {
				Some("%s not found, try sbt update.".format(lwjglJar))
			}
		}
	} describedAs "Copy all LWJGL natives to the right position."

	lazy val cleanLwjgl = task {
		FileUtilities.clean(nativeLibPath, log)
		None
	}

  lazy val makeExec = task {
    val native = outputPath / "native"
    val libs = outputPath / "libs"
    val zipName = "%s.zip".format(this.projectName.value)
    val outZip = outputPath / zipName 
  
    // Remove existing zip if it's there
    FileUtilities.clean(outZip, log)

    // Copy all the jars we need
    FileUtilities.copyFlat(requiredRuntime, libs, log)
   
    // Extract the libs we need
    FileUtilities.unzip(compilePath / "%s.jar".format(lwjglJar), native, log) 
    val natives = native ** "*.*"
    FileUtilities.copyFlat(natives.get, outputPath, log)

    // Remove native folder
    FileUtilities.clean(native, log)
    native.asFile.delete

    // We don't want these
    val filtered = List(mainResourcesOutputDirectoryName, 
                        "classes", 
                        "analysis", 
                        "test-analysis", 
                        zipName) map { dir =>
      (outputPath / dir).asInstanceOf[PathFinder]
    } reduceLeft(_ +++ _)
    // Archive the app
    val archived = (outputPath ##) * "*" --- filtered
    val resources = (mainResourcesOutputPath ##) * "*"
    FileUtilities.zip(archived.get ++ resources.get, outZip, true, log)

    // Final bit of cleanup
    FileUtilities.clean(libs, log)
    FileUtilities.clean(archived.get, log)
    None
  } dependsOn(`package`) describedAs "Creates an archive for binary executable."
  
  // Maybe make this configurable
  def requiredRuntime = List(buildLibraryJar, compilePath / "lwjgl-%s.jar".format(lwjglVersion)) 
  def manifestedJars = requiredRuntime map ("libs/" + _.name) mkString(" ")
  override def manifestClassPath = Some(manifestedJars)

	def lwjglJar = "lwjgl-native-%s" format(lwjglVersion)
	def lwjglVersion = "2.7.1"

	override def copyResourcesAction = super.copyResourcesAction dependsOn copyLwjgl

  // Removing the java.library.path addition, as
  // this could only cause the double loading
  // error... Will revisit this if this implementation
  // becomes a problem (though I don't see how it could).
	def nativeLWJGLPath = defineOs._1 match {
		case "unknown" => ""
		case _ => nativeLibPath / defineOs._1
	}
        
	override def fork = {
		forkRun(("-Djava.library.path=" + nativeLWJGLPath) :: Nil)
	}

}

/**
 * JMonkey, unfortunately, does not have a public maven repo
 * We are going to hack the dependency by pulling down their
 * nightly builds and extracting the dependecies we need.
 */
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

  def createIfNotExists(d: Path) = if(!d.exists) FileUtilities.createDirectory(d, log)

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
  
  override def updateAction = 
    super.updateAction dependsOn jmonkeyCache
}

/**
 * Slick dependencies
 */
trait Slick2D extends LWJGLProject { 
	lazy val slickRepo = "Slick2D Maven Repo" at "http://slick.cokeandcode.com/mavenrepo"

	// Mainly for slick stuff
	lazy val b2srepo = "b2srepo" at "http://b2s-repo.googlecode.com/svn/trunk/mvn-repo"
	lazy val freeheprepo = "Freehep" at "http://java.freehep.org/maven2"

	lazy val slick = "slick" % "slick" % slickVersion 

  // Patch unfortunately can't depend on update because
  // update will fail leaving the file to be patched behind
	lazy val `patch` = task {
		val path = "%s/.ivy2/cache/phys2d/phys2d/ivy-060408.xml" format(System.getProperty("user.home"))
		new java.io.File(path) exists match {
		case true =>
			log.info("Patching %s ..." format(path))
			val pattern = "zip".r
			val ivysource = Source.fromFile(path)
			val text = ivysource.getLines.mkString
			val writer = new java.io.FileWriter(path)
			writer.write(pattern.replaceAllIn(text, "jar"))
			writer.close
			log.info("Done.")
      None
		case false =>
			log.warn("Update might fail. This is expected.")
			log.warn("Please run update one more time.")
      None
		}
	} describedAs "Patchs the phys2d dependency xml file"

  // Override this for newer version
  def slickVersion = "274"

  override def requiredRuntime = 
    super.requiredRuntime ++ List(compilePath / "slick-%s.jar".format(slickVersion)) 
  
	override def updateAction = 
		super.updateAction dependsOn `patch`

}
