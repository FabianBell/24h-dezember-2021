package com.senacor.models;

import com.senacor.websocket.MQController;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MQSpec {

  String taskName;
  String responseName;
}
