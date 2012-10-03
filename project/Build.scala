import sbt._
import Keys._

import sbtassembly.Plugin._
import AssemblyKeys._

object Build extends sbt.Build
{
  import Dependencies._

  lazy val myProject = Project("numonitor", file(".")).settings(
    (assemblySettings ++ Seq(
      organization  := "com.olchovy",
      version       := "0.1.0",
      scalaVersion  := "2.9.1",
      scalacOptions := Seq("-deprecation", "-encoding", "utf8"),

      mainClass     in assembly := Some("com.olchovy.numonitor.cep.Boot"),
      jarName       in assembly := "topology.jar",
      excludedJars  in assembly <<= (fullClasspath in assembly) map { classpath => 
        classpath.filter(_.data.getName.startsWith("kafka-standalone"))
      },

      TaskKey[File]("local-topology") <<= (baseDirectory, fullClasspath in Compile, mainClass in assembly) map {
        (basedir, classpath, main) =>
          val template = "#!/bin/sh\njava -classpath \"%s\" %s \"$@\"\n"
          val mainString = main.getOrElse(sys.error("No main class specified"))
          val filteredClasspath = classpath.filterNot(_.data.getName.startsWith("kafka-standalone"))
          val contents = template.format(filteredClasspath.files.absString, mainString)
          val out = basedir / "bin/run-local-topology.sh"
          IO.write(out, contents)
          out.setExecutable(true)
          out
      },

      resolvers           ++= Dependencies.repos,
      libraryDependencies ++= provided(storm) ++ compile(stormKafka, jedis) ++ test(scalatest)
    )): _*
  )
}

