# sbt-lwjgl-plugin

This is a simple plugin for sbt specifically for [LWJGL] projects. The idea was taken from this post by [Death by Misadventure].

Please visit the [wiki] for more info, roadmap, etc.

**Note: ** This plugin was recently migrated over to sbt 0.10, so project migration information will trickle in slowly.

## Usage

In your project, create a project/plugins/build.sbt, whose contents are as follows:

    libraryDependencies += "com.github.philcali" %% "xsbt-lwjgl-plugin" % "0.0.1"

**Or**, you can make use of the giter8 template, to kick off your project.

    g8 philcali/lwjgl.g8

Once you answer all the appropriate questions, you will have a lwjgl project in awaiting.

## How it works

The plugin makes use of public maven repo found at the LWJGL wiki. It pulls the dependencies, and extracts lwjgl jar to your working lib directory.

**Note for Slick Devs**: You may have to run update twice. The second time will patch an ivy dependency xml to pull the correct phys2d jar.

See the [wiki] for more detail about satellite project definition (ie: [Slick2D], jMonkey, [Nicol], [Ardor3D])

[Ardor3D]: http://ardor3d.com/
[Nicol]: http://scan.github.com/Nicol
[Slick2D]: http://slick.cokeandcode.com/
[wiki]: https://github.com/philcali/sbt-lwjgl-plugin/wiki/sbt-lwjgl-plugin
[Death by Misadventure]: http://blog.misadventuregames.com/post/248744147/scala-and-lwjgl-with-sbt-updated
[LWJGL]: http://lwjgl.org/
