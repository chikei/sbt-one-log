package io.github.chikei.sbt.onelog

import java.io.PrintWriter

import com.zavakid.sbt.LogDepProcess.{ProcessContext, _}
import net.virtualvoid.sbt.graph.DependencyGraphKeys._
import net.virtualvoid.sbt.graph.{DependencyGraphPlugin, ModuleGraph, ModuleId}
import org.fusesource.scalate.TemplateEngine
import sbt.Keys._
import sbt.{AutoPlugin, Def, IO, Load, ModuleID, Plugins, Project, ProjectRef, Scoped, State, Task, _}

object OneLog extends AutoPlugin {

  import OneLogKeys._

  val autoImport = OneLogKeys

  override def requires: Plugins = DependencyGraphPlugin

  override def trigger = allRequirements

  override def globalSettings: Seq[Def.Setting[_]] = {
    onLoad := onLoad.value andThen doTask
  }

  var appended = false
  val doTask: State => State = { state =>
    if (OneLog.appended)
      state
    else {
      import state.globalLogging.{full => log}

      val buildStruct = Project.structure(state)
      val extracted = Project.extract(state)
      if (!extracted.getOpt(oneLogDisableDependencyProcess in Global).getOrElse(false)) {
        log.info("sbt-one-log start process...")

        def compute(graph: ModuleGraph, libraryDeps: Seq[sbt.ModuleID], p: ProjectRef): IndexedSeq[ModuleID] = {
          val roots = graph.nodes.filter(n => !graph.edges.exists(_._2 == n.id)).sortBy(_.id.idString)
          val directDeps = roots.flatMap(d => graph.dependencyMap(d.id))
            .filter(_.evictedByVersion.isEmpty)
            .filterNot(d => d.id.organisation.equals("org.scala-lang"))
            .flatMap { dep => // filter deps which not contains sub projects
              libraryDeps.find { libDep =>
                libDep.organization.equals(dep.id.organisation) && libDep.name.equals(dep.id.name)
              }.map(dep -> _)
            }

          directDeps.foldLeft(libraryDeps.toIndexedSeq) {
            case (libs, (dep, libDep)) =>
              val context = ProcessContext(dep.id, libDep, graph, libs, p, extracted)
              processStrategies(context).libraryDeps
          }
        }

        val (transformed, newState) = buildStruct.allProjectRefs.filter { p =>
          //FIXME! .task is deprecated
          val t = oneLogComputeModuleGraph in p
          val s = Scoped.scopedSetting(t.scope, t.key)
          extracted.getOpt(s).isDefined
          //extracted.getOpt((computeModuleGraph in p).task).isDefined
        }.foldLeft((extracted.session.mergeSettings, state)) { case ((allSettings, foldedState), p) =>
          // need receive new state
          val (newState, depGraph) = extracted.runTask(oneLogComputeModuleGraph in p, foldedState)
          val newLibs = compute(depGraph, extracted.get(libraryDependencies in p), p)
          (allSettings.map {
            s =>
              s.key.key match {
                //case s if "libraryDependencies".equals(s.key.key.label) =>
                case libraryDependencies.key =>
                  // ensure just modify this project's dependencies
                  s.key.scope.project.toOption.filter(p.equals(_)).fold(s.asInstanceOf[Setting[Seq[ModuleID]]]) { _ =>
                    s.asInstanceOf[Setting[Seq[ModuleID]]].mapInit((_, _) => newLibs)
                  }
                case _ => s
              }
          }, newState)
        }

        OneLog.appended = true
        //extracted.append(appendedSettings, state)
        val newStructure = Load.reapply(transformed, extracted.structure)(extracted.showKey)
        log.info("sbt-one-log finished process")
        Project.setProject(extracted.session, newStructure, newState)
      } else state
    }
  }

  //  def task: State => State = { state =>
  //    val extracted = Project.extract(state)
  //    extracted.structure.allProjectRefs.foldLeft(state) { (state, p) =>
  //      val ds: Seq[ModuleID] = extracted.get(libraryDependencies in p)
  //      println("=====" + p + " dep : ")
  //      ds.foreach(println)
  //      println("===========")
  //      if (p.project == "module1") {
  //        val (newState, _) = extracted.runTask(update.in(p).in(Compile), state)
  //        extracted.append(Seq[Setting[_]](
  //          libraryDependencies in p := Seq()
  //        ), newState)
  //      } else state
  //
  //    }
  //  }

  override def projectSettings: Seq[Setting[_]] = Seq[Setting[_]](
    oneLogSlf4jVersion := "1.7.10",
    oneLogLogbackVersion := "1.1.2",
    oneLogScalaLoggingVersion := (scalaBinaryVersion.value match {
      case "2.12" => "3.5.0"
      case "2.11" => "3.5.0"
      case "2.10" => "2.1.2"
    }),
    oneLogUseScalaLogging := true,
    resolvers += "99-empty" at "http://version99.qos.ch/",
    oneLogComputeModuleGraph := (moduleGraph in Compile).value
  ) ++ inConfig(Compile) {
    Seq(
      oneLogLogbackXmlTemplate := "/sbtonelog/templates/logback.xml.mustache",
      oneLogLogbackFileName := "logback.xml",
      oneLogGenerateLogbackXml := generateLogbackXMLImpl.value
    )
  } ++ inConfig(Test) {
    Seq(
      oneLogLogbackXmlTemplate := "/sbtonelog/templates/logback-test.xml.mustache",
      oneLogLogbackFileName := "logback-test.xml",
      oneLogGenerateLogbackXml := generateLogbackXMLImpl.value
    )
  } ++ Seq(Compile, Test, Runtime, Provided, Optional).flatMap{ config =>
    inConfig(config)(Seq(
      oneLogComputeChanges :=
          Changes.generate(moduleGraph.value, libraryDependencies.value,
            oneLogSlf4jVersion.value, oneLogLogbackVersion.value,
            scalaBinaryVersion.value, oneLogScalaLoggingVersion.value),
      oneLogRenderOverride := ChangesLayout.sbtRenderOverride,
      oneLogRenderDependency := ChangesLayout.sbtRenderAdd,
      ongLogChangesAscii := ChangesLayout.ascii(oneLogComputeChanges.value, oneLogRenderDependency.value, oneLogRenderOverride.value),
      oneLogChanges := {
        val s = streams.value
        s.log.info(name.value)
        s.log.info(ongLogChangesAscii.value)
      },
      oneLogWriteChangesFile := target.value / ("changes-%s.txt").format(config.toString),
      oneLogWriteChange := {
        val f = oneLogWriteChangesFile.value
        val p = new PrintWriter(f, "UTF-8")
        p.write(ChangesLayout.ascii(oneLogComputeChanges.value, oneLogRenderDependency.value, oneLogRenderOverride.value,
          false))
        p.close()
      }
    ))
  }

  lazy val generateLogbackXMLImpl: Def.Initialize[Task[Unit]] = Def.task {
    val out = streams.value

    def generateContent(engine: TemplateEngine, context: Map[String, Any], templatePath: String, baseDir: File, file: File) {
      val content = engine.layout(templatePath, context)
      if (!baseDir.exists) baseDir.mkdirs()
      file.createNewFile()
      out.log.info(s"generate $file")
      IO.write(file, content)
    }

    //val force = generateLogbackXMLParser.parsed
    val force = false
    val resourceDir = resourceDirectory.value
    val logbackXML = resourceDir / oneLogLogbackFileName.value
    val context = Map("projectName" -> name.value)
    val engine = new TemplateEngine()
    (force, logbackXML.exists()) match {
      case (false, false) =>
        generateContent(engine, context, oneLogLogbackXmlTemplate.value, resourceDir, logbackXML)
      case (false, true) =>
        out.log.info(s"${logbackXML.toString} is exist")
      case (true, _) =>
        out.log.warn(s"force generate is not support yes")
    }
  }
}
