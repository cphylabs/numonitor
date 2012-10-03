package com.olchovy.numonitor.serializer

import java.nio.ByteBuffer
import kafka.message.Message
import kafka.serializer.Encoder

class IntEncoder extends Encoder[Int]
{
  override def toMessage(i: Int) = new Message(ByteBuffer.allocate(4).putInt(i).array) 
}
