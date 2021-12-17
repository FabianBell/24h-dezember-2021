package de.untitledcoworkers.imageservice;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

import java.util.concurrent.TimeoutException;
import java.io.IOException;

import java.util.Map;

/**
 * Hello world!
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

        // final Channel channel;

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
