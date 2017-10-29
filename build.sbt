lazy val scalaV = "2.12.4"

resolvers += "jitpack" at "https://jitpack.io"

lazy val server = (project in file("server")).settings(
  scalaVersion := scalaV,
  scalaJSProjects := Seq(client),
  pipelineStages in Assets := Seq(scalaJSPipeline),
  pipelineStages := Seq(digest, gzip),
  // triggers scalaJSPipeline when using compile or continuous compilation
  compile in Compile <<= (compile in Compile) dependsOn scalaJSPipeline,
  libraryDependencies ++= Seq(
    "com.vmunier" %% "scalajs-scripts" % "1.1.1",
    guice,
    filters,
    ws,
    "com.typesafe.akka" %% "akka-testkit" % "2.5.6" % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.6" % Test,
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
    "org.awaitility" % "awaitility" % "3.0.0" % Test
  )
).enablePlugins(PlayScala)
  .dependsOn(sharedJvm)

lazy val client = (project in file("client")).settings(
  scalaVersion := scalaV,
  scalaJSUseMainModuleInitializer := true,
  scalacOptions ++= Seq("-Xmax-classfile-name","78"),
  scalaJSUseMainModuleInitializer in Test := false,
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
 // jsDependencies += "org.webjars" % "flot" % "0.8.3" / "flot.js" minified "flot.min.js",
 // jsDependencies += "org.webjars" % "bootstrap" % "3.3.6" / "bootstrap.js" minified "bootstrap.min.js",
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.9.1",
    "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
    "com.typesafe.play" %%% "play-json" % "2.6.1",
    "com.thoughtworks.binding" %%% "dom" % "11.0.0-M4",
    "com.thoughtworks.binding" %%% "futurebinding" % "11.0.0-M4",
    "fr.hmil" %%% "roshttp" % "2.0.2",
    "org.scala-js" %%% "scalajs-java-time" % "0.2.2"
   // "com.github.fancellu" % "scalajs-flot_sjs0.6_2.11" % "0.4"
  )
).enablePlugins(ScalaJSPlugin, ScalaJSWeb).
  dependsOn(sharedJs)

lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared"))
  .settings(scalaVersion := scalaV
    , libraryDependencies ++= Seq(
      "org.julienrf" %%% "play-json-derived-codecs" % "4.0.0"
      , "biz.enef" %%% "slogging" % "0.6.0"
    ))
  .jsConfigure(_ enablePlugins ScalaJSWeb)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

// loads the server project at sbt startup
onLoad in Global := (Command.process("project server", _: State)) compose (onLoad in Global).value
