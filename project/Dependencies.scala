import sbt._

object Dependencies
{
  val repos = Seq(
    "Typesafe repo [releases]" at "http://repo.typesafe.com/typesafe/releases/",
    "clojars.org" at "http://clojars.org/repo"
  )

  def provided  (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "provided")
  def compile   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")
  def test      (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")
  def runtime   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "runtime")

  val scalatest   = "org.scalatest"  %% "scalatest"   % "1.6.1"
  val storm       = "storm"          %  "storm"       % "0.8.1"
  val stormKafka  = "storm"          %  "storm-kafka" % "0.8.0-wip4"
  val jedis       = "redis.clients"  %  "jedis"       % "1.5.2"
}
