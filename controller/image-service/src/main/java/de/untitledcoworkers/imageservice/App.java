package de.untitledcoworkers.imageservice;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

import java.util.concurrent.TimeoutException;
import java.io.IOException;

import java.util.Map;

/**
 * This is the Controller.
 *
 * This will use a WebSocket to communicate with a Frontend thing and use
 * RabbitMQ Client Things to distribute the tasks to model things.
 *
 *  1. Receive request on websocket from frontend --- Who send which call, map and save it for later.
 *  2. Read what to do and in which order ----------- First decisisions and forwarding.
 *  3. Send to each channel with a modell thingy ---- Comm. channel to send others
 *  4. Receive modified picture bytes or so. -------- Comm. channel to consume from channels.
 *  5. Send back to corresponding frontend. --------- Back to connected frontend.
 */
public class App
{
    private final static String QUEUE_NAME = "hello";

    public static void main( String[] args )
    {
        ConnectionFactory factory = new ConnectionFactory();

        factory.setHost("localhost");
        // factory.setUsername();
        // factory.setPassword();
        // factory.setPort();

        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            boolean durable = false;
            boolean exlclusive = false;
            boolean autoDelete = false;
            Map<String, Object> arguments = null;
            channel.queueDeclare(QUEUE_NAME, durable, exlclusive, autoDelete, arguments);

            String message = "Hello world";

            /// SEND

            String arg1 = "";
            channel.basicPublish(arg1, QUEUE_NAME, null, message.getBytes());
            System.out.println("[x] Sent '"  + message + "'");

            /// ---------------------------------------------------------------
            /// RECEIVE (then)
            //
            // DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            //     String message = new String(delivery.getBod(), "UTF-8");
            //     System.out.println("[x] Received '" + message + "'");
            // };
            // boolean autoAck = true;
            // channel.basicConsume(QUEUE_NAME, autoAck, deliverCallback, consumerTag -> { });

        } catch (IOException|TimeoutException e) {
            System.err.println("Yeah, there's someting strange." + e.toString());
            e.printStackTrace();
        }
    }
}
