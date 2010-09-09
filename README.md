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

The plugin simply looks for lwjgl in your lib directory of your project. If you already have lwjgl native libraries defined in your JAVA_LIBRARY_PATH,
then you're golden.

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
