package com.olchovy.numonitor.consumer

import kafka.consumer.{ConsumerConnector, Whitelist}
import com.olchovy.numonitor.common.Service
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

