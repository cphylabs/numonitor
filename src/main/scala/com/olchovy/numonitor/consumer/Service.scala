package com.olchovy.numonitor.consumer

import kafka.consumer.{ConsumerConnector, Whitelist}
import com.olchovy.numonitor.actor.ExpiringBoundedQueue
import com.olchovy.numonitor.common._
import com.olchovy.numonitor.serializer.IntDecoder

private[consumer] class ConsumerService(consumer: ConsumerConnector) extends Service
{
  override val topic = "number"

  override def start {
    val topicFilter = new Whitelist(topic)
    val streams = consumer.createMessageStreamsByFilter(topicFilter, numStreams = 1, decoder = new IntDecoder)
    for(event <- streams.head) println(event.message)
  }

  override def shutdown = consumer.shutdown
}

private[consumer] class BatchConsumerService(consumer: ConsumerConnector, batchSize: Int) extends Service
{
  override val topic = "number"

  val timeout = 1000L * 60

  val queue = ExpiringBoundedQueue[Int](batchSize, timeout)(q => println("flushing %d items".format(q.size)))

  override def start {
    val topicFilter = new Whitelist(topic)
    val streams = consumer.createMessageStreamsByFilter(topicFilter, numStreams = 1, decoder = new IntDecoder)
    for(event <- streams.head) queue ! ExpiringBoundedQueue.Enqueue[Int](event.message)
  }

  override def shutdown {
    queue.stop
    consumer.shutdown
  }
}

