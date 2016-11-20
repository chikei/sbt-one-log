package io.github.chikei.sbt.onelog

import net.virtualvoid.sbt.graph.ModuleGraph
import sbt.{TaskKey, _}

object SbtOneLogKeys {

  val oneLogSlf4jVersion = settingKey[String]("which slf4j version to use")
  val oneLogLogbackVersion = settingKey[String]("which logback version to use")
  //val scalaLoggingVersion = settingKey[String]("which scalaLogging version to use")
  val oneLogUseScalaLogging = settingKey[Boolean]("add the scalaLogging(https://github.com/typesafehub/scala-logging)")
  val oneLogLogbackXmlTemplate = settingKey[String]("the logback template path")
  val oneLogLogbackFileName = settingKey[String]("the logback file name")
  //val logbackTestXMLTemplate = settingKey[String]("the logback-test template path")
  val oneLogWithLogDependencies = settingKey[Seq[sbt.ModuleID]]("with log dependencies")
  val oneLogGenerateLogbackXml = taskKey[Unit]("generate logback.xml and logback-test.xml in test if they are not exist")
  val oneLogComputeModuleGraph = taskKey[ModuleGraph]("Compute the dependency graph for a project")
}
