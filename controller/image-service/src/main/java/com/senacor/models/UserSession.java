package com.senacor.models;

import lombok.Builder;
import lombok.Data;

import javax.websocket.Session;
import java.util.List;

@Data
@Builder
public class UserSession {
  List<String> methods;
  String img;
  String extensions;
  Session socket;
  int count;
}
