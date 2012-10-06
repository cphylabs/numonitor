package com.olchovy.numonitor.consumer

import kafka.consumer.{Consumer, ConsumerConfig}
import com.olchovy.numonitor.common.Service

object Boot
{
  def main(args: Array[String]) {
    Service.buildFromProperties("/consumer.properties") { props =>
      val consumerConfig = new ConsumerConfig(props)
      val consumer = Consumer.create(consumerConfig)

      if(props.getProperty("service.batch", "off") == "off")
        new ConsumerService(consumer)
      else
        new BatchConsumerService(consumer, 1000)
    } match {
      case Right(service) => service.start
      case Left(error) => System.err.println(error.getMessage)
    }
  }
}
