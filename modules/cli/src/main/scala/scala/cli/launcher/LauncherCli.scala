package scala.cli.launcher
import dependency._

import scala.build.internal.{OsLibc, Runner}
import scala.build.options.{BuildOptions, JavaOptions}
import scala.build.{Artifacts, Positioned}
import scala.cli.commands.LoggingOptions

object LauncherCli {

  def runAndExit(version: String, options: LauncherOptions, remainingArgs: Seq[String]): Nothing = {

    val logger       = LoggingOptions().logger
    val scalaVersion = options.cliScalaVersion.getOrElse("2.12")

    val scalaCliDependency = Seq(dep"org.virtuslab.scala-cli:cli_$scalaVersion:$version")
    val snapshotsRepo =
      Seq(coursier.Repositories.sonatype("snapshots").root, coursier.Repositories.central.root)

    val fetchedScalaCli =
      Artifacts.fetch(
        Positioned.none(scalaCliDependency),
        snapshotsRepo,
        ScalaParameters(scalaVersion),
        logger,
        None
      ) match {
        case Right(value) => value
        case Left(value) =>
          System.err.println(value.message)
          sys.exit(1)
      }

    val scalaCli =
      fetchedScalaCli.fullDetailedArtifacts.collect { case (_, _, _, Some(f)) =>
        f.toPath.toFile
      }

    val buildOptions = BuildOptions(
      javaOptions = JavaOptions(
        jvmIdOpt = Some(OsLibc.baseDefaultJvm(OsLibc.jvmIndexOs, "17"))
      )
    )

    val exitCode =
      Runner.runJvm(
        buildOptions.javaHome().value.javaCommand,
        buildOptions.javaOptions.javaOpts.toSeq.map(_.value.value),
        scalaCli,
        "scala.cli.ScalaCli",
        remainingArgs,
        logger,
        allowExecve = true
      )

    sys.exit(exitCode)
  }

}
