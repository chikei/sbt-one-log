package io.github.chikei.sbt.onelog

import net.virtualvoid.sbt.graph.{ModuleGraph, ModuleId}
import sbt.{TaskKey, _}

object OneLogKeys {

  val oneLogSlf4jVersion = settingKey[String]("which slf4j version to use")
  val oneLogLogbackVersion = settingKey[String]("which logback version to use")
  val oneLogScalaLoggingVersion = settingKey[String]("which scalaLogging version to use")
  val oneLogUseScalaLogging = settingKey[Boolean]("add the scalaLogging(https://github.com/typesafehub/scala-logging)")
  val oneLogLogbackXmlTemplate = settingKey[String]("the logback template path")
  val oneLogLogbackFileName = settingKey[String]("the logback file name")
  //val logbackTestXMLTemplate = settingKey[String]("the logback-test template path")
  val oneLogWithLogDependencies = settingKey[Seq[sbt.ModuleID]]("with log dependencies")
  val oneLogGenerateLogbackXml = taskKey[Unit]("generate logback.xml and logback-test.xml in test if they are not exist")
  val oneLogComputeModuleGraph = taskKey[ModuleGraph]("Compute the dependency graph for a project")
  val oneLogDisableDependencyProcess = settingKey[Boolean]("disable auto dependency processing")
  val oneLogComputeChanges = taskKey[Updates]("Compute updating instruction")
  val ongLogChangesAscii = taskKey[String]("Updating instruction in string")
  val oneLogChanges = taskKey[Unit]("Print updating instruction to build configuration for unifying logging")
  val oneLogRenderOverride = taskKey[(ModuleId) => String]("How to render override")
  val oneLogRenderDependency = taskKey[(ModuleId) => String]("How to render dependency")
  val oneLogWriteChangesFile = taskKey[File]("File location of oneLogChangesWrite task")
  val oneLogWriteChange = taskKey[Unit]("Write updating instruction to file")
}
