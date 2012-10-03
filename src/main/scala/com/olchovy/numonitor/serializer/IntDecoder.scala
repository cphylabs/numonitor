package com.olchovy.numonitor.serializer

import java.nio.ByteBuffer
import kafka.message.Message
import kafka.serializer.Decoder

class IntDecoder extends Decoder[Int]
{
  override def toEvent(message: Message) = message.payload.getInt
}
