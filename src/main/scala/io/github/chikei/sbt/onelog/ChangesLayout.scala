package io.github.chikei.sbt.onelog

import net.virtualvoid.sbt.graph.ModuleId

object ChangesLayout {
  //excludes:
  //  dep:
  //  exclude("org", "name")
  //  exclude("org", "name")
  //addes:
  // libraryDepdencies += add_1
  // libraryDepdencies += add_2
  //overrides:
  // dependencyOverrides += dep1
  // dependencyOverrides += dep2
  def ascii(changes: Updates, renderAdd: ModuleId => String, renderOverride: ModuleId => String, ident: Boolean = true): String = {
    val twoSpace = if(ident) " " + " " else ""
    implicit val ordering: Ordering[ModuleId] = Ordering.by(id => (id.organisation, id.name, id.version))

    def excludes(ex: Map[ModuleId, Set[ModuleId]]): String = {
      ex.toSeq.sortBy(_._1).flatMap { case (k, v) =>
        Vector(s""""$twoSpace${k.organisation}" % "${k.name}":""") ++
          v.toSeq.sorted.map(e =>
            s"""$twoSpace${twoSpace}exclude("${e.organisation}", "${e.name}")"""
          )
      }.mkString("\n")
    }
    def addes(add: Set[ModuleId], render: ModuleId => String): String = {
      add.toSeq.sorted.map(a =>
        twoSpace + "libraryDependencies += " + render(a)
      ).mkString("\n")
    }
    def overrides(over: Map[ModuleId, String], render: ModuleId => String): String = {
      over.toSeq.sorted.map { case (k, v) =>
        twoSpace + "dependencyOverrides += " + render(k.copy(version = v))
      }.mkString("\n")
    }

    val ex = excludes(changes.excludes)
    val add = addes(changes.addes, renderAdd)
    val over = overrides(changes.overrides, renderOverride)
    ((if (ex.isEmpty) Vector.empty else Vector(ex, "")) ++
      (if (add.isEmpty) Vector.empty else Vector(add, "")) ++
      (if (over.isEmpty) Vector.empty else Vector(over))
      ).mkString("\n")
  }

  val sbtRenderAdd = (m: ModuleId) => s""""${m.organisation}" % "${m.name}" % "${m.version}""""
  val sbtRenderOverride = (m: ModuleId) => {
    val ver = if (m.organisation == "com.typesafe.scala-logging") "%%" else "%"
    s""""${m.organisation}" % "${m.name}" $ver "${m.version}""""
  }
}
