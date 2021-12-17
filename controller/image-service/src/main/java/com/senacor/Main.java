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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.senacor.models.MQSpec;
import com.senacor.models.UserSession;
import com.senacor.websocket.RestoreController;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
@ServerEndpoint("/websocket")
public class Main {

    private HashMap<String, MQSpec> mqSpecMap;
    private Connection connection;
    private HashMap<String, Channel> channelMap;

    public Main() throws IOException, TimeoutException {
        System.out.println("STARTED");
        mqSpecMap = new HashMap<>();
        mqSpecMap.put("GFPGAN", MQSpec.builder()
            .taskName("FACE_RESTORE_TASK")
            .responseName("FACE_RESTORE_RESPONSE").build());

        ConnectionFactory factory = new ConnectionFactory();
        String host = System.getenv("MQ_HOST");
        if (host == null) host = "localhost";
        factory.setHost(host);
        connection = factory.newConnection();

        channelMap = new HashMap<>();
        for (Map.Entry<String, MQSpec> entry : mqSpecMap.entrySet()){
            Channel taskChannel = connection.createChannel();
            taskChannel.queueDeclare(entry.getValue().getTaskName(), false, false, false, null);
            Channel responseChannel = connection.createChannel();
            responseChannel.queueDeclare(entry.getValue().getResponseName(), false, false, false, null);
            channelMap.put(entry.getKey(), taskChannel);
            DeliverCallback callback = (consumerTag, delivery) -> {
                handleResponse(delivery.getBody());
            };
            taskChannel.basicConsume(entry.getValue().getResponseName(), false, callback, consumerTag -> {});
        }
    }

    private void handleResponse(byte[] body) throws IOException {
        JsonObject response = new JsonObject(new String(body));
        String id = response.getString("sessionkey");
        String img = response.getString("image");
        UserSession userSession = sessions.get(id);
        userSession.setImg(img);
        performMethod(id);
    }

    Map<String, UserSession> sessions = new ConcurrentHashMap<>();
    
    
    private void performMethod(String id) throws IOException {
        UserSession userSession = sessions.get(id);
        if (userSession.getMethods().size() == 0){
            sendResponse(id);
        }
        String nextMethod = userSession.getMethods().get(0);
        userSession.getMethods().remove(0);
        JsonObject body = new JsonObject();
        body.put("image", userSession.getImg());
        body.put("extension", userSession.getExtensions());
        body.put("sessionkey", id);
        switch (nextMethod){
            case "GFPGAN":
                channelMap.get("GFPGAN").basicPublish("", mqSpecMap.get("GFPGAN").getTaskName(), null, body.toString().getBytes());
                break;
            default:
                throw new RuntimeException("Unknown message type");
        }
    }

    private void sendResponse(String id){
        UserSession userSession = sessions.get(id);
        userSession.getSocket().getAsyncRemote().sendText(userSession.getImg());
    }

    @OnMessage
    public void onMessage(Session session, String message) throws IOException {
        JsonObject body = new JsonObject(message);
        JsonArray jsonMethods = body.getJsonArray("models");
        List<String> methods = IntStream.range(0, jsonMethods.size()).mapToObj(jsonMethods::getString).collect(Collectors.toList());
        String id = body.getString("session");
        String img = body.getString("img");
        String extension = body.getString("extension");
        UserSession userSession = UserSession.builder().methods(methods).img(img).extensions(extension).socket(session).build();
        sessions.put(id, userSession);
        performMethod(id);
    }
}