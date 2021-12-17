package com.senacor;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.senacor.models.MQSpec;
import com.senacor.websocket.RestoreController;

@ApplicationScoped
@ServerEndpoint("/websocket")
public class Main {

    private HashMap<String, MQSpec> mqSpecMap;
    private Connection connection;
    private HashMap<String, Channel> channelMap;

    public Main() throws IOException, TimeoutException {
        System.out.println("STARTED");
        mqSpecMap = new HashMap<>();
        mqSpecMap.put("restore", MQSpec.builder()
            .taskName("FACE_RESTORE_TASK")
            .responseName("FACE_RESTORE_RESPONSE")
            .controller(new RestoreController()).build());

        ConnectionFactory factory = new ConnectionFactory();
        String host = System.getenv("MQ_HOST");
        if (host == null) host = "localhost";
        factory.setHost(host);
        connection = factory.newConnection();

        channelMap = new HashMap<>();
        for (MQSpec mqSpec : mqSpecMap.values()){
            Channel taskChannel = connection.createChannel();
            taskChannel.queueDeclare(mqSpec.getTaskName(), false, false, false, null);
            Channel responseChannel = connection.createChannel();
            responseChannel.queueDeclare(mqSpec.getResponseName(), false, false, false, null);
            channelMap.put(mqSpec.getTaskName(), taskChannel);
            channelMap.put(mqSpec.getResponseName(), responseChannel);
            mqSpec.getController().setResponseChannel(responseChannel);
            DeliverCallback callback = (consumerTag, delivery) -> {
                mqSpec.getController().consume(delivery.getBody());
            };
            taskChannel.basicConsume(mqSpec.getResponseName(), false, callback, consumerTag -> {});
        }
    }

    @OnMessage
    public void onMessage(Session session, String message) throws IOException {
        session.getAsyncRemote().sendText("HALLO");
    }
}