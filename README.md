sbt-lwjgl-plugin
================

This is a simple plugin for sbt specifically for [LWJGL] projects. The idea was taken from this
post by [Death by Misadventure].

Usage
---

In your project, create a plugins/Plugins.scala, whose contents are as follows:

    import sbt._
    
    class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
      val lwjglPlugin = "scan" % "sbt-lwjgl-plugin" % "0.3" from "http://github.com/scan/sbt-lwjgl-plugin/downloads/sbt-lwjgl-plugin-0.3.jar"
    }

How it works
---

The plugin adds the necessary dependencies for LWJGL to your project. On `run`, the downloaded natives are extracted to the lib directory, the `java.library.path` is set correctly.

The plugin comes with a Slick2D trait for [Slick] dependencies.

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

[Slick]: http://slick.cokeandcode.com/index.php
[Death by Misadventure]: http://blog.misadventuregames.com/post/248744147/scala-and-lwjgl-with-sbt-updated
[LWJGL]: http://lwjgl.org
