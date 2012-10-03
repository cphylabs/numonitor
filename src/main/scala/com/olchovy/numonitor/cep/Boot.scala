package com.olchovy.numonitor.cep

import backtype.storm._

object Boot
{
  def main(args: Array[String]) {
    val config = new Config
    config.setDebug(true)

    if(args != null && args.length > 0) {
      config.setNumWorkers(5)
      config.setMaxSpoutPending(5000)
      val builder = TopologyBuilder.KafkaSpout
      StormSubmitter.submitTopology(args(0), config, builder.build)
    } else {
      val cluster = new LocalCluster
      val builder = TopologyBuilder.FixedBatchSpout

      sys.addShutdownHook {
        cluster.killTopology(builder.name)
        cluster.shutdown
      }

      cluster.submitTopology(builder.name, config, builder.build)
    }
  }
}

