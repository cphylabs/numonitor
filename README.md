numonitor
=========
Kafka and Storm + Trident integration.

`com.olchovy.numonitor.producer`
--------------------------------
A simple Kafka producer.

Every second, the `ProducerService` will publish a random integer between 0 and 10000 to the *number* topic.

`com.olchovy.numonitor.consumer`
--------------------------------
A simple Kafka consumer.

Prints, to stdout, integers sent to topic *number*.

`com.olchovy.numonitor.cep`
---------------------------
Two Storm + Trident topologies exist in this package.

The topologies are functionally equivalent with the exception of their spouts and their deployment strategies.

The `FixedBatchSpout` topology will be run on a local Storm cluster (see the generated `bin/run-local-topology.sh` after issuing `sbt local-topology`)

The `KafkaSpout` topology will be run when the assembled `topology.jar` file is deployed remotely.

Each spout will produce tuples with one field ("number") which contains an integer value between 0 and 10000.

The topology will filter these integers, retaining all prime numbers.

The retained primes will be stored in a Trident `State` implementation that is backed by a Bloom filter.

The Bloom filter is persisted in Redis and can be queried via Trident or independently.
