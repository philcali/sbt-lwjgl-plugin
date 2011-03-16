sbt-lwjgl-plugin
================

This is a simple plugin for sbt specifically for [LWJGL] projects. The idea was taken from this
post by [Death by Misadventure].

Usage
---

In your project, create a plugins/Plugins.scala, whose contents are as follows:

    import sbt._
    
    class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
      val lwjglPlugin = "calico" % "sbt-lwjgl-plugin" % "1.0" from "http://github.com/downloads/philcali/sbt-lwjgl-plugin/sbt-lwjgl-plugin-1.0.jar"
    }

How it works
---

The plugin makes use of public [maven repo] found at the LWJGL wiki. It pulls the dependencies, and extracts to your
lib directory.

The plugin comes with a Slick2D trait for [Slick] dependencies.


**Note on Slick**: The phys2d dependency for Slick says it's a zip, when in fact it is a jar.
Simply edit your `~/ivy/cache/phys2d/phys2d/ivy-*.xml` and replace the zip's to jar. 

Example Project Definition
---

Here's an example project definition:

    import sbt._
  
    class ExampleProject(info: ProjectInfo) extends LWJGLProject(info) with Slick2D {
      // If you choose to overwrite the native LWJGL path, you can do so here.
      // An example of this would be if you defined the path as a separate env variable
      // Note: this override is optional
      override def nativeLWJGLPath = System.getProperty("different.path")
    }

[maven repo]: http://www.lwjgl.org/wiki/index.php?title=LWJGL_use_in_Maven
[Slick]: http://slick.cokeandcode.com/index.php
[Death by Misadventure]: http://blog.misadventuregames.com/post/248744147/scala-and-lwjgl-with-sbt-updated
[LWJGL]: http://lwjgl.org
