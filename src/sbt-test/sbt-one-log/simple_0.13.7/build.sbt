import java.io.File

lazy val simple_0_13_7 = (project in file("."))

version := "0.1"

scalaVersion := "2.11.5"
ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

libraryDependencies ++= Seq(
  "org.mybatis" % "mybatis" % "3.2.7", //dependent slf4j-log4j12
  "commons-beanutils" % "commons-beanutils" % "1.9.1", //dependent commons-logging
  "log4j" % "log4j" % "1.2.17"
)


oneLogSlf4jVersion := "1.7.5"

oneLogLogbackVersion := "1.0.3"

val expected = Set[String](
  "org.slf4j:log4j-over-slf4j:1.7.5"
  ,"org.slf4j:jcl-over-slf4j:1.7.5"
  ,"org.slf4j:jul-to-slf4j:1.7.5"
  ,"org.slf4j:slf4j-api:1.7.5"
  ,"ch.qos.logback:logback-classic:1.0.3"
  ,"ch.qos.logback:logback-core:1.0.3"
  ,"commons-logging:commons-logging:99-empty"
  ,"commons-logging:commons-logging-api:99-empty"
  ,"log4j:log4j:99-empty"
  ,"com.typesafe.scala-logging:scala-logging_2.11:3.1.0"
)

val excluded = Set[(String,String)](
  "org.slf4j" -> "slf4j-log4j12",
  "org.slf4j" -> "slf4j-jcl",
  "org.slf4j" -> "slf4j-jdk14"
)

TaskKey[Unit]("check") <<= (allDeps) map { deps =>
  val all = deps.map(_.toString).toSet
  expected.foreach{ ept =>
    if(!all.contains(ept)) error(s"libraryDependencies [$ept] error!")
  }
  //if(!expected.subsetOf(deps.map(_.toString).toSet))
  //  error("libraryDependencies error!")
  deps.map(d => (d.organization,d.name)).foreach{ d =>
    if(excluded.contains(d)) error(s"excludeDependencies [$d] error!")
  }
  ()
}

TaskKey[Unit]("deleteLogback") := {
  val resourceDir = (resourceDirectory in Compile).value
  val resourceDirInTest = (resourceDirectory in Test).value
  val logbackXML = resourceDir / "logback.xml"
  val logbackTestXML = resourceDirInTest / "logback-test.xml"
  logbackXML.delete()
  logbackTestXML.delete()
}

TaskKey[Unit]("checkLogback") := {
  val resourceDir = (resourceDirectory in Compile).value
  val resourceDirInTest = (resourceDirectory in Test).value
  val logbackXML = resourceDir / "logback.xml"
  val logbackTestXML = resourceDirInTest / "logback-test.xml"
  if(!logbackXML.exists()) error(s"$logbackXML is not generated")
  if(!logbackTestXML.exists()) error(s"$logbackTestXML is not generated")
}

val allDeps = taskKey[Seq[ModuleID]]("get all dependency with transivate modueID")

allDeps <<= (externalDependencyClasspath in Compile) map {
  cp =>
    cp.flatMap(_.get(Keys.moduleID.key))
}
