import ScalateKeys._

sbtPlugin := true
organization := "io.github.chikei"
name := "sbt-one-log"
licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))
organizationHomepage := Some(new URL("https://github.com/chikei/sbt-one-log/"))
description := "A sbt plugin for uniform log lib"
scalaVersion := "2.10.6"
scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked")
publishArtifact in Test := false
crossPaths := false
incOptions := incOptions.value.withNameHashing(true)
publishMavenStyle := false
bintrayRepository := "sbt-plugins"
bintrayOrganization in bintray := None

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

libraryDependencies += "org.fusesource.scalate" %% "scalate-core" % "1.6.1"
libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.5"

scalateSettings
scalateTemplateConfig in Compile := Seq(TemplateConfig((sourceDirectory in Compile).value / "templates", Nil, Nil, Some("sbtonelog.templates")))

ScriptedPlugin.scriptedSettings
scriptedLaunchOpts := { scriptedLaunchOpts.value ++
  Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value)
}
scriptedBufferLog := false
