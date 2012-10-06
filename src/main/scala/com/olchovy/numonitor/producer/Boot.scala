package com.olchovy.numonitor.producer

import kafka.producer.{Producer, ProducerConfig}
import com.olchovy.numonitor.common.Service

object Boot
{
  def main(args: Array[String]) {
    Service.buildFromProperties("/producer.properties") { props =>
      val producerConfig = new ProducerConfig(props)
      val producer = new Producer[String, Int](producerConfig)

      if(props.getProperty("service.batch", "off") == "off")
        new ProducerService(producer)
      else
        new BatchProducerService(producer)
    } match {
      case Right(service) => service.start
      case Left(error) => System.err.println(error.getMessage)
    }
  }
}
