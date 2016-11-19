import sbt._
import sbt.Keys._
import com.zavakid.sbt._
import SbtOneLogKeys._

object Build extends sbt.Build {

  val commonSettings = Defaults.defaultSettings ++
    Seq(
     scalaVersion := "2.11.5"
     ,ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }
     ,organization := "com.me.module1_2.11"
     ,version := "0.1"
     ,resolvers ++= Seq(
       "typesafe" at "http://repo.typesafe.com/typesafe/releases/"
       ,"datanucleus" at "http://www.datanucleus.org/downloads/maven2/"
     )

  )

  lazy val root = Project(
    id = "multi_0_13_7",
    base = file("."),
    settings = commonSettings ++ Seq(
        // custom settings here
    )
  ).aggregate(module1, module2, module3)

  lazy val module1 = Project(
    id = "module1",
    base = file("module1"),
    settings = commonSettings ++ Seq(
      libraryDependencies := libraryDependencies.value ++ Seq(
        "org.mybatis" % "mybatis" % "3.2.7" //dependent slf4j-log4j12
        ,"commons-beanutils" % "commons-beanutils" % "1.9.1" //dependent commons-logging
        ,"com.alibaba.otter" % "node.deployer" % "4.2.11"
      ),
      slf4jVersion := "1.7.7"
    )
  )

  lazy val module2 = Project(
    id = "module2",
    base = file("module2"),
    settings = commonSettings ++ Seq(
      libraryDependencies := libraryDependencies.value ++ Seq(
        "commons-beanutils" % "commons-beanutils" % "1.9.1" //dependent commons-logging
        ,"org.apache.commons" % "commons-dbcp2" % "2.0.1"
      )
    )
  )

  lazy val module3 = Project(
    id = "module3",
    base = file("module3"),
    settings = commonSettings ++ Seq(
      libraryDependencies := libraryDependencies.value ++ Seq(
        "log4j" % "log4j" % "1.2.17" 
      )
    )
  )
}
