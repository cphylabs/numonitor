package com.olchovy.numonitor.cep

import scala.collection.JavaConversions._
import backtype.storm.generated.StormTopology
import backtype.storm.tuple.{Fields, Values}
import backtype.storm.spout.Scheme
import storm.kafka._
import storm.trident.TridentTopology
import storm.trident.operation.BaseFilter
import storm.trident.spout._
import storm.trident.testing.FixedBatchSpout
import storm.trident.tuple.TridentTuple

trait TopologyBuilder[SpoutType]
{
  val name: String

  val spout: SpoutType

  def build: StormTopology
}

object TopologyBuilder
{
  import com.olchovy.numonitor.cep.state.BloomFilterState
  import com.olchovy.numonitor.common._

  val FixedBatchSpout = new TopologyBuilder[FixedBatchSpout] {
    val name = "self-serving-cep"

    val spout = new FixedBatchSpout(new Fields("number"), 100, Seq.fill(1000)(randomValue): _*)

    def build = {
      val topology = new TridentTopology
      val numberField = new Fields("number")

      topology.newStream("numbers", spout) 
        .each(numberField, new PrimeFilter)
        .partitionPersist(BloomFilterState.Factory("primes"), numberField, BloomFilterState.Updater)

      topology.build
    }

    private def randomValue = new Values(util.Random.nextInt(Config.Max + 1): java.lang.Integer)
  }

  val KafkaSpout = new TopologyBuilder[KafkaSpout] {
    val name = "cep"

    val spout = {
      import java.nio.ByteBuffer

      val numberScheme = new Scheme {
        def deserialize(bytes: Array[Byte]) = {
          val buffer = ByteBuffer.wrap(bytes)
          new Values(buffer.getInt: java.lang.Integer)
        }

        def getOutputFields = new Fields("number")
      }

      val hosts = {
        val hostStrings: java.util.List[String] = List("%s:%d".format(Config.KafkaHost, Config.KafkaPort))
        val partitionsPerHost = 1
        KafkaConfig.StaticHosts.fromHostString(hostStrings, partitionsPerHost)
      }

      val topic = "number"

      val config = new SpoutConfig(hosts, topic, "/storm-kafka", "number-spout")
      config.scheme = numberScheme

      new KafkaSpout(config)
    }

    def build = {
      val topology = new TridentTopology
      val numberField = new Fields("number")

      topology.newStream("numbers", spout)
        .each(numberField, new PrimeFilter)
        .partitionPersist(BloomFilterState.Factory("primes"), numberField, BloomFilterState.Updater)

      topology.build
    }
  }

  private[cep] class PrimeFilter extends BaseFilter
  {
    private lazy val primeStream: Stream[Int] = { 
      def helper(stream: Stream[Int]): Stream[Int] = { 
        Stream.cons(stream.head, helper(stream.tail.filter(_ % stream.head != 0)))
      }   

      helper(Stream.from(2))
    }

    private lazy val primeSet = primeStream.take(1000).toSet

    def isKeep(tuple: TridentTuple): Boolean = primeSet.contains(tuple.getInteger(0))
  }
}
