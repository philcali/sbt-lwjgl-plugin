sbt-lwjgl-plugin
================

This is a simple plugin for sbt specifically for [LWJGL] projects. The idea was taken from this
post by [Death by Misadventure].

Usage
---

In your project, create a plugins/Plugins.scala, whose contents are as follows:

    import sbt._
    
    class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
      val lwjglPlugin = "com.github.philcali" % "sbt-lwjgl-plugin" % "2.0"
    }

How it works
---

The plugin makes use of public [maven repo] found at the LWJGL wiki. It pulls the dependencies, and extracts lwjgl 
jar to your working lib directory.


The plugin comes with a Slick2D trait for [Slick] dependencies.


**Note for Slick Devs**: You may have to run update twice. The second time will
patch an ivy dependency xml to pull the correct phys2d jar.

Example Project Definition
---

Here's an example project definition:

    import sbt._
  
    class ExampleProject(info: ProjectInfo) extends LWJGLProject(info) with Slick2D {
       // Optionally override the lwjglVersion for newer versions
      override def lwjglVersion = "2.7"
    }

[maven repo]: http://www.lwjgl.org/wiki/index.php?title=LWJGL_use_in_Maven
[Slick]: http://slick.cokeandcode.com/index.php
[Death by Misadventure]: http://blog.misadventuregames.com/post/248744147/scala-and-lwjgl-with-sbt-updated
[LWJGL]: http://lwjgl.org
