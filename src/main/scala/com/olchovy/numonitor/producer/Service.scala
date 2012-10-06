package com.olchovy.numonitor.producer

import kafka.producer._
import com.olchovy.numonitor.common._

private[producer] class ProducerService(producer: Producer[String, Int]) extends Service
{
  override val topic = "number"

  override def start = for(n <- Stream.continually(util.Random.nextInt(Config.Max + 1))) {
    send(n)
    println("sent: %d".format(n))
    Thread.sleep(1000)
  }

  protected def send(int: Int) = producer.send(new ProducerData[String, Int](topic, int))
}

private[producer] class BatchProducerService(producer: Producer[String, Int]) extends ProducerService(producer)
{
  override def start = {
    for(i <- 1 to Config.Max) {
      val n = util.Random.nextInt(Config.Max + 1)
      send(n)
      println("sent: %d (%d / %d)".format(n, i, Config.Max))
    }

    shutdown
  }
}
