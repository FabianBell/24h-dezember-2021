package com.senacor.websocket;

import com.rabbitmq.client.Channel;
import lombok.Setter;

public abstract class MQController {

  @Setter
  Channel responseChannel;

  public abstract void consume(byte[] data);
}
