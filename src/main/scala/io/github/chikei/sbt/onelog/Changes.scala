package io.github.chikei.sbt.onelog

import net.virtualvoid.sbt.graph.{Module, ModuleGraph, ModuleId}
import sbt.ModuleID
import sbt._

case class Updates(
                    addes: Set[ModuleId] = Set.empty,
                    excludes: Map[ModuleId, Set[ModuleId]] = Map.empty,
                    overrides: Map[ModuleId, String] = Map.empty
                  ) {
  def merge(that: Updates): Updates = {
    val ex = {
      val common = this.excludes.keySet intersect that.excludes.keySet
      val result = this.excludes ++ that.excludes
      if (common.isEmpty) result
      else {
        common.foldLeft(result) { case (r, k) =>
          r + (k -> (this.excludes(k) ++ that.excludes(k)))
        }
      }
    }
    Updates(this.addes ++ that.addes, ex, this.overrides ++ that.overrides)
  }
}

case class UpdatesContext(graph: ModuleGraph,
                          scalaBinVersion: String,
                          slf4jVersion: String,
                          logbackVersion: String,
                          scalaLoggingVersion: String)

object Changes {
  type GenerateStrategy = (UpdatesContext, Module) => Updates

  def generate(graph: ModuleGraph,
               libraryDependencies: Seq[ModuleID],
               slf4jVersion: String,
               logbackVersion: String,
               scalaBinaryVersion: String,
               scalaLoggingVersion: String): Updates = {
    val deps = graph.dependencyMap
    // should be only one root (project)
    val roots = graph.roots

    val directDeps = roots.flatMap(d => deps(d.id))
      //.filter(_.evictedByVersion.isEmpty)
      .filterNot(d => d.id.organisation == "org.scala-lang")
      .filter(dep => // filter sub projects
        libraryDependencies.exists(lib => lib.organization == dep.id.organisation &&
          lib.name == dep.id.name)
      )

    val ctx = UpdatesContext(graph, scalaBinaryVersion, slf4jVersion, logbackVersion, scalaLoggingVersion)
    directDeps.foldLeft(Updates()) { case (u, dep) =>
      u merge generateStrategies(ctx, dep)
    }
  }

  def generateStrategies: GenerateStrategy = (ctx: UpdatesContext, m: Module) => {
    val ex = exclusiveStrategis(ctx, m)
    val scalaLogging = scalaLoggingStrategy(ctx, m)
    val slf4jOverride = slf4jStrategy(ctx, m)
    val logbackOverride = logbackStrategy(ctx, m)
    val add = if(ex.addes.nonEmpty || ex.overrides.nonEmpty) {
      Seq(ModuleId("org.slf4j", "slf4j-api", ctx.slf4jVersion))
    } else Seq.empty[ModuleId]

    val overrides =
      ex.overrides ++
      scalaLogging.overrides ++
      slf4jOverride.overrides ++
      logbackOverride.overrides ++
      (if(add.isEmpty) Map.empty else Map(slf4jApi -> ctx.slf4jVersion))

    Updates(ex.addes ++ add, ex.excludes, overrides)
  }

  def exclusiveStrategis: GenerateStrategy = (ctx: UpdatesContext, m: Module) =>
    Seq(commonLoggingStrategy, log4jStrategy, julStrategy)
      .foldLeft(Updates()) { case (u, gs) =>
      u.merge(gs(ctx, m))
    }

  val slf4jApi = ModuleId("org.slf4j", "slf4j-api", "")
  val logbackClassic = ModuleId("ch.qos.logback", "logback-classic", "")

  val slf4jStrategy: GenerateStrategy = (ctx: UpdatesContext, m: Module) => {
    val over = m.id overrideVersion(ctx.graph, slf4jApi.copy(version = ctx.slf4jVersion))
    Updates(overrides = over)
  }

  val logbackStrategy: GenerateStrategy = (ctx: UpdatesContext, m: Module) => {
    val core = ModuleId("ch.qos.logback", "logback-core", ctx.logbackVersion)
    val classic = logbackClassic.copy(version = ctx.logbackVersion)
    val over = (m.id overrideVersion(ctx.graph, core)) ++
      (m.id overrideVersion(ctx.graph, classic))
    Updates(overrides = over)
  }

  // just override version
  val scalaLoggingStrategy: GenerateStrategy = (ctx: UpdatesContext, m: Module) => {
    def post210 = {
      val sl = {
        val sbtID = "com.typesafe.scala-logging" %% "scala-logging" % ctx.scalaLoggingVersion
        ModuleId(sbtID.organization, sbtID.name, sbtID.revision)
      }
      m.id overrideVersion(ctx.graph, sl)
    }

    val overrides = ctx.scalaBinVersion match {
      case "2.12" => post210
      case "2.11" => post210
      case "2.10" => {
        val api = {
          val sbtID = "com.typesafe.scala-logging" %% "scala-logging-api" % ctx.scalaLoggingVersion
          ModuleId(sbtID.organization, sbtID.name, sbtID.revision)
        }
        val slf4j = {
          val sbtID = "com.typesafe.scala-logging" %% "scala-logging-slf4j" % ctx.scalaLoggingVersion
          ModuleId(sbtID.organization, sbtID.name, sbtID.revision)
        }

        (m.id overrideVersion(ctx.graph, api)) ++
          (m.id overrideVersion(ctx.graph, slf4j))
      }
    }

    val withLogback: Map[ModuleId, String] =
      if(overrides.nonEmpty) overrides ++ Map(logbackClassic -> ctx.logbackVersion)
      else Map.empty

    Updates(overrides = withLogback)
  }

  // 1. exclude any "org.slf4j" % "slf4j-log4j12"
  // 2. override any "log4j" % "log4j to version99
  // 3. override any "org.slf4j" % "log4j-over-slf4j" to our version
  // 4. if any log4j-over-slf4j overridden, assemble exclude and override as updates
  // 5. or if any result of 1 & 2 is non-empty, assemble 1 & 2 with adding log4j-over-slf4j
  val log4jStrategy: GenerateStrategy = (ctx: UpdatesContext, m: Module) => {
    val log4j = ModuleId("log4j", "log4j", "99-empty")
    val slf4jLog4j = ModuleId("org.slf4j", "slf4j-log4j12", "")
    val slf4j = ModuleId("org.slf4j", "log4j-over-slf4j", "")

    val ex = m.id exclude(ctx.graph, slf4jLog4j)

    val over = m.id overrideVersion(ctx.graph, log4j)

    val slf4jOverride = m.id overrideVersion(ctx.graph, slf4j)

    if(slf4jOverride.nonEmpty) Updates(Set.empty, ex, over ++ slf4jOverride)
    else {
      if(ex.isEmpty && over.isEmpty) Updates()
      else Updates(Set(slf4j), ex, over)
    }
  }

  // 1. exclude any "org.slf4j" % "slf4j-jcl"
  // 2. override any "commons-logging" % "commons-logging" or "commons-logging" % "commons-logging-api" to version99
  // 3. override any "org.slf4j" % "jcl-over-slf4j" to our version
  // 4. if any jcl-over-slf4j overridden, assemble exclude and override as updates
  // 5. or if any result of 1 & 2 is non-empty, assemble 1 & 2 with adding jcl-over-slf4j
  val commonLoggingStrategy: GenerateStrategy = (ctx: UpdatesContext, m: Module) => {
    val jcl = ModuleId("commons-logging", "commons-logging", "99-empty")
    val jclApi = ModuleId("commons-logging", "commons-logging-api", "99-empty")
    val slf4jJcl = ModuleId("org.slf4j", "slf4j-jcl", "")
    val slf4j = ModuleId("org.slf4j", "jcl-over-slf4j", ctx.slf4jVersion)

    val ex = m.id exclude(ctx.graph, slf4jJcl)

    val over = (m.id overrideVersion(ctx.graph, jcl)) ++
      (m.id overrideVersion(ctx.graph, jclApi))

    val slf4jOverride = m.id overrideVersion(ctx.graph, slf4j)

    if (slf4jOverride.nonEmpty) Updates(Set.empty, ex, over ++ slf4jOverride)
    else {
      if (ex.isEmpty && over.isEmpty) Updates()
      else Updates(Set(slf4j), ex, over)
    }
  }

  // 1. exclude any "org.slf4j" % "slf4j-jdk14"
  // 2. override any "org.slf4j" % "jul-to-slf4j" to our version
  // 3. if we excluded jdk14 but not override any jul bridge means there is no bridge, add jul bridge
  val julStrategy: GenerateStrategy = (ctx: UpdatesContext, m: Module) => {
    val jdk = ModuleId("org.slf4j", "slf4j-jdk14", "")
    val jul = ModuleId("org.slf4j", "jul-to-slf4j", ctx.slf4jVersion)

    val ex = m.id.exclude(ctx.graph, jdk)

    val over = m.id overrideVersion(ctx.graph, jul)

    val add =
      if (ex.nonEmpty && over.isEmpty) Set(jul)
      else Set.empty[ModuleId]

    Updates(add, ex, over)
  }

  implicit private class ModuleIDCompare(val underlying: ModuleId) {
    def sameVersionAs(that: ModuleId): Boolean = {
      (underlying sameArtifactAs that) &&
        underlying.version == that.version
    }

    def sameArtifactAs(that: ModuleId): Boolean = {
      underlying.organisation == that.organisation &&
        underlying.name == that.name
    }

    def dependsOnArtifact(graph: ModuleGraph, artifact: ModuleId): Boolean = {
      def search(deps: Seq[ModuleId]): Boolean = deps.exists { mid =>
        if (artifact sameArtifactAs mid) true
        else search(graph.dependencyMap(mid).map(_.id))
      }

      search(Seq(underlying))
    }

    def dependsOnDifferentVersion(graph: ModuleGraph, artifact: ModuleId): Boolean = {
      def search(deps: Seq[ModuleId]): Boolean = deps.exists { mid =>
        if ((artifact sameArtifactAs mid) && artifact.version != mid.version) true
        else search(graph.dependencyMap(mid).map(_.id))
      }

      search(Seq(underlying))
    }

    def overrideDiffVersion(graph: ModuleGraph, overrideWith: ModuleId): Map[ModuleId, String] = {
      if (dependsOnDifferentVersion(graph, overrideWith)) Map(overrideWith -> overrideWith.version)
      else Map.empty
    }

    def overrideVersion(graph: ModuleGraph, overrideWith: ModuleId): Map[ModuleId, String] = {
      if(dependsOnArtifact(graph, overrideWith)) Map(overrideWith.copy(version = "") -> overrideWith.version)
      else Map.empty
    }

    def exclude(graph: ModuleGraph, exclusion: ModuleId): Map[ModuleId, Set[ModuleId]] = {
      if (dependsOnArtifact(graph, exclusion)) Map(underlying -> Set(exclusion))
      else Map.empty
    }

    def addIfNotDepends(graph: ModuleGraph, dep: ModuleId): Set[ModuleId] = {
      if(dependsOnArtifact(graph, dep)) Set.empty
      else Set(dep)
    }
  }

}
