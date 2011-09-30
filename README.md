# sbt-lwjgl-plugin

This is a simple plugin for sbt specifically for [LWJGL] projects. The idea was taken from this post by [Death by Misadventure].

Please visit the [wiki] for more info, roadmap, etc.

## Usage

In your project, create a project/plugins/build.sbt, whose contents are as follows:

```
addSbtPlugin("com.github.philcali" % "sbt-lwjgl-plugin" % "3.1.0")
```

To take advantage of the plugin's settings, you must add `LWJGLPlugin.lwjglSettings` either to your build.sbt or build.scala.
For working in a particular child environment, use one of the satellite settings, ie: `Nicol.nicolSettings` or `JMonkeyProject.jmonkeySettings`. 

**Or**, you can make use of the giter8 template, to kick off your project.

    g8 philcali/lwjgl.g8

Once you answer all the appropriate questions, you will have a lwjgl project in awaiting.

## How it works

The plugin makes use of public maven repo found at the LWJGL wiki. It pulls the dependencies, and extracts the 
lwjgl jar containing OS specific natives to your `managedResource` directory.

**Note for Slick Devs**: You may have to run update twice. The second time will patch an ivy dependency xml to pull the correct phys2d jar.

See the [wiki] for more detail about satellite project definition (ie: [Slick2D], [jMonkey], [Nicol], [Ardor3D])

[Ardor3D]: http://ardor3d.com/
[Nicol]: http://scan.github.com/Nicol
[jMonkey]: http://jmonkeyengine.org/
[Slick2D]: http://slick.cokeandcode.com/
[wiki]: https://github.com/philcali/sbt-lwjgl-plugin/wiki/sbt-lwjgl-plugin
[Death by Misadventure]: http://blog.misadventuregames.com/post/248744147/scala-and-lwjgl-with-sbt-updated
[LWJGL]: http://lwjgl.org/
