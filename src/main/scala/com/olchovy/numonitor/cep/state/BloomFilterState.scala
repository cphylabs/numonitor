package com.olchovy.numonitor.cep.state

import java.math.BigInteger
import scala.collection.JavaConversions._
import scala.collection.mutable.BitSet
import redis.clients.jedis._
import storm.trident.operation.TridentCollector
import storm.trident.state._
import storm.trident.tuple.TridentTuple
import com.olchovy.bloomfilter._
import com.olchovy.numonitor.common.Service

case class BloomFilterState(key: String, filter: BloomFilter[Int]) extends State
{
  import BloomFilterState._

  def beginCommit(txid: java.lang.Long) {
    // no-op 
  }

  def commit(txid: java.lang.Long) {
    // no-op
  }

  def add(numbers: Traversable[Int]) {
    numbers.foreach(filter.add _)
    Store.persist(this)
  }
}

object BloomFilterState
{
  case class Factory(key: String) extends StateFactory
  {
    import Factory._

    def makeState(config: java.util.Map[_, _], partitionIndex: Int, numPartitions: Int): State = {
      Store.fetch(key).getOrElse {
        val state = newInstance
        Store.persist(state)
        state
      }
    }

    private def newInstance = new BloomFilterState(key, BloomFilter[Int](capacity, fpp))
  }

  object Factory
  {
    val capacity = 250

    val fpp = 0.01
  }

  object Updater extends BaseStateUpdater[BloomFilterState]
  {
    def updateState(state: BloomFilterState, tuples: java.util.List[TridentTuple], collector: TridentCollector) {
      val numbers = tuples.map(_.getInteger(0).toInt)
      state.add(numbers)
    }
  }

  object Store
  {
    private val service: RedisService = Service.buildFromProperties("/redis.properties") { props =>
      val host = props.getProperty("host", "localhost")
      val port = props.getProperty("port", "6379").toInt
      new RedisService(host, port)
    } match {
      case Right(redisService) => redisService
      case Left(e) => throw e
    }

    def fetch(key: String): Option[BloomFilterState] = {
      val result = service.execute(_.get(key))
      Option(result).map(deserialize(key, _))
    }

    def persist(state: BloomFilterState) = service.execute { client => 
      (client.set _).tupled(serialize(state))
    }

    def serialize(state: BloomFilterState): (String, String) = {
      val bytes = state.filter.serialize
      state.key -> bytes.map("%02x".format(_)).mkString
    }

    def deserialize(key: String, value: String): BloomFilterState = {
      val bytes = value.grouped(2).map(Integer.parseInt(_, 16).toByte).toArray
      BloomFilterState(key, BloomFilter.deserialize(bytes))
    }

    private class RedisService(host: String, port: Int) extends Service
    {
      private lazy val pool: JedisPool = new JedisPool(new JedisPoolConfig, host, port)

      override def shutdown = pool.destroy

      def execute[T](f: Jedis => T) = {
        val client = pool.getResource

        try {
          f(client)
        } finally {
          pool.returnResource(client)
        }
      }
    }
  }
}
