import java.io._
import java.util.jar._

object JarExtractor {
	private def copyStream(istream: InputStream, ostream: OutputStream) : Unit = {
		var bytes =  new Array[Byte](1024)
		var len = -1
		while({ len = istream.read(bytes, 0, 1024); len != -1 })
		ostream.write(bytes, 0, len)
	}

	def extractJar(file: File, directory: String) : Unit = {
		val basename = file.getName.substring(0, file.getName.lastIndexOf("."))
		val todir = if(directory == "") new File(file.getParentFile, basename) else new File(directory + "/" + basename)
		todir.mkdirs()

		println("Extracting " + file + " to " + todir)
		val jar = new JarFile(file)
		val enu = jar.entries
		while(enu.hasMoreElements) {
			val entry = enu.nextElement
			val entryPath = if(entry.getName.startsWith(basename)) entry.getName.substring(basename.length)
				else entry.getName

			println("Extracting to " + todir + "/" + entryPath)
			if(entry.isDirectory) {
				new File(todir, entryPath).mkdirs
			} else {
				val istream = jar.getInputStream(entry)
				val ostream = new FileOutputStream(new File(todir, entryPath))
				copyStream(istream, ostream)
				ostream.close
				istream.close
			}
		}
	}

	def apply(filename: String): Unit = extractJar(new File(filename), "")

	def apply(filename: String, directory: String): Unit = extractJar(new File(filename), directory)
}