import sbt.Project.projectToRef

lazy val clients = Seq(client)
lazy val scalaV = "2.11.8"

lazy val server = (project in file("server")).settings(
  name := """web-autosys-prototype-1""",
  version := "1.0-SNAPSHOT",
  scalaVersion := scalaV,
  scalaJSProjects := clients,
  pipelineStages := Seq(scalaJSProd, gzip),
  resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  libraryDependencies ++= Seq(
    "com.vmunier" %% "play-scalajs-scripts" % "0.5.0"
    ,"org.webjars" % "jquery" % "1.11.1"
    ,specs2 % Test
    ,cache
    ,ws
    ,evolutions
    ,"org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0-RC1" % Test
    ,"com.typesafe.play" %% "play-slick" % "2.0.0"
    ,"com.typesafe.play" %% "play-slick-evolutions" % "2.0.0"
    ,"org.apache.commons" % "commons-pool2" % "2.4.2"
    ,"com.jcraft" % "jsch" % "0.1.53"
  )
).enablePlugins(PlayScala).
  aggregate(clients.map(projectToRef): _*).
  dependsOn(sharedJvm)

lazy val client = (project in file("client")).settings(
  scalaVersion := scalaV,
  persistLauncher := true,
  persistLauncher in Test := false,
  libraryDependencies ++= Seq(
    "be.doeraene" %%% "scalajs-jquery" % "0.9.0"
  )
).enablePlugins(ScalaJSPlugin, ScalaJSPlay).
  dependsOn(sharedJs)

lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared")).
  settings(scalaVersion := scalaV).
  jsConfigure(_ enablePlugins ScalaJSPlay)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

// loads the Play project at sbt startup
onLoad in Global := (Command.process("project server", _: State)) compose (onLoad in Global).value
