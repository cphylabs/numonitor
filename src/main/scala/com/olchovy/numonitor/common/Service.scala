package com.olchovy.numonitor.common

trait Service
{
  val topic = ".*"

  def start: Unit = {}

  def shutdown: Unit = {}

  sys.addShutdownHook { shutdown }
}

object Service
{
  import java.util.Properties

  def buildFromProperties[A <: Service](filename: String)(f: Properties => A): Either[Throwable, A] = {
    Option(getClass.getResourceAsStream(filename)) match {
      case Some(inputStream) => try {
        val props = new Properties
        props.load(inputStream)
        Right(f(props))
      } catch {
        case e => Left(e)
      } finally {
        inputStream.close
      }

      case None => Left(new Error("Required file \"%s\" could not be found on the classpath.".format(filename)))
    }
  }
}
