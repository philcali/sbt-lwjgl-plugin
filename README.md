# sbt-lwjgl-plugin

This is a simple plugin for sbt specifically for [LWJGL] projects. The idea was taken from this post by [Death by Misadventure].

Please visit the [wiki] for more info, roadmap, etc.

## Usage

In your project, create a plugins/Plugins.scala, whose contents are as follows:

    import sbt._

    class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
      val lwjglPlugin = "com.github.philcali" % "sbt-lwjgl-plugin" % "2.0.5"
    }

**Or**, you can make use of the giter8 template, to kick off your project.

    g8 philcali/lwjgl.g8

Once you answer all the appropriate questions, you will have a lwjgl project in awaiting.

## How it works

The plugin makes use of public maven repo found at the LWJGL wiki. It pulls the dependencies, and extracts lwjgl jar to your working lib directory.

The plugin comes with a [Slick2D] trait for Slick dependencies.

**Note for Slick Devs**: You may have to run update twice. The second time will patch an ivy dependency xml to pull the correct phys2d jar.

## Example Project Definition

Here's an example project definition:

    import sbt._

    // Plain LWJGL Project
    class ExampleProject(info: ProjectInfo) extends LWJGLProject(info)

See the [wiki] for more detail about satellite project definition (ie: Slick2d, jMonkey)

[Slick2D]: http://slick.cokeandcode.com/
[wiki]: https://github.com/philcali/sbt-lwjgl-plugin/wiki/sbt-lwjgl-plugin
[Death by Misadventure]: http://blog.misadventuregames.com/post/248744147/scala-and-lwjgl-with-sbt-updated
[LWJGL]: http://lwjgl.org/
