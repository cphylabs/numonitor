package com.olchovy.numonitor.producer

import kafka.producer._
import com.olchovy.numonitor.common._

private[producer] class ProducerService(producer: Producer[String, Int]) extends Service
{
  override val topic = "number"

  override def start = for(n <- Stream.continually(util.Random.nextInt(Config.Max + 1))) {
    send(n)
    Thread.sleep(1000)
  }

  override def shutdown = producer.close

  private def send(int: Int) = producer.send(new ProducerData[String, Int](topic, int))
}
