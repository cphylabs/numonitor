package com.olchovy.numonitor.consumer

import kafka.consumer.{Consumer, ConsumerConfig}
import com.olchovy.numonitor.common.Service

object Boot
{
  def main(args: Array[String]) {
    Service.buildFromProperties("/consumer.properties") { props =>
      val consumerConfig = new ConsumerConfig(props)
      val consumer = Consumer.create(consumerConfig)
      new ConsumerService(consumer)
    } match {
      case Right(service) => service.start
      case Left(error) => System.err.println(error.getMessage)
    }
  }
}
