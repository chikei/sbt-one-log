package io.github.chikei.sbt.onelog

import net.virtualvoid.sbt.graph.ModuleGraph
import sbt.{TaskKey, _}

object SbtOneLogKeys {

  val slf4jVersion = settingKey[String]("which slf4j version to use")
  val logbackVersion = settingKey[String]("which logback version to use")
  //val scalaLoggingVersion = settingKey[String]("which scalaLogging version to use")
  val useScalaLogging = settingKey[Boolean]("add the scalaLogging(https://github.com/typesafehub/scala-logging)")
  val logbackXMLTemplate = settingKey[String]("the logback template path")
  val logbackFileName = settingKey[String]("the logback file name")
  //val logbackTestXMLTemplate = settingKey[String]("the logback-test template path")
  val withLogDependencies = settingKey[Seq[sbt.ModuleID]]("with log dependencies")
  val generateLogbackXML = TaskKey[Unit]("generate-logback-xml", "generate logback.xml and logback-test.xml in test if they are not exist")
  val computeModuleGraph = TaskKey[ModuleGraph]("compute-module-graph", "Compute the dependency graph for a project")
}
