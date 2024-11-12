package com.sample.application;

import com.solacesystems.jcsmp.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CountDownLatch;

@Service
public class SolaceSubscriberService {

    @Value("${solace.host}")
    private String host;

    @Value("${solace.clientUsername}")
    private String username;

    @Value("${solace.clientPassword}")
    private String password;

    @Value("${solace.msgVpn}")
    private String vpn;

    @Value("${solace.topic}")
    private String topic;

    @Value("${solace.queue}")
    private String queue;

    private JCSMPSession session;
    private XMLMessageConsumer consumer;
    private FlowReceiver QueueConsumer;

    public void connect() {
        try {
            // Create the connection factory
            JCSMPProperties properties = new JCSMPProperties();
            properties.setProperty(JCSMPProperties.HOST, host);
            properties.setProperty(JCSMPProperties.USERNAME, username);
            properties.setProperty(JCSMPProperties.PASSWORD, password);
            properties.setProperty(JCSMPProperties.VPN_NAME, vpn);

            // Create a session
            session = JCSMPFactory.onlyInstance().createSession(properties);
            session.connect();

        } catch (JCSMPException e) {
            e.printStackTrace();
        }
    }

    public void startSubcribeTopic() {
        try {
            final CountDownLatch latch = new CountDownLatch(1);

            consumer = session.getMessageConsumer(new XMLMessageListener() {

                @Override
                public void onReceive(BytesXMLMessage msg) {
                    // your implementation here....
                    if (msg instanceof TextMessage) {
                        System.out.printf("TextMessage received: '%s'%n",
                                ((TextMessage)msg).getText());
                    } else {
                        System.out.println("Message received.");
                    }
                    System.out.printf("Message Dump:%n%s%n",msg.dump());
                    latch.countDown();  // unblock main thread
                }

                @Override
                public void onException(JCSMPException e) {
                    System.out.printf("Consumer received exception: %s%n",e);
                    latch.countDown();  // unblock main thread
                }
            });

            session.addSubscription(JCSMPFactory.onlyInstance().createTopic(topic), true);
            // Start the consumer
            consumer.start();

        } catch (JCSMPException e) {
            e.printStackTrace();
        }
    }

    public void stopSubcribeTopic() {
        if (consumer != null) {
            consumer.stop();
        }
        if (session != null) {
            session.closeSession();

            // session.removeSubscription(JCSMPFactory.onlyInstance().createTopic(topic), true)

        }
    }

    public void startReceivingFromQueue() throws JCSMPException {
        Queue q = JCSMPFactory.onlyInstance().createQueue(queue);
        final ConsumerFlowProperties flow_prop = new ConsumerFlowProperties();
        flow_prop.setEndpoint(q);
        flow_prop.setAckMode(JCSMPProperties.SUPPORTED_MESSAGE_ACK_CLIENT);

        EndpointProperties endpoint_props = new EndpointProperties();
        endpoint_props.setAccessType(EndpointProperties.ACCESSTYPE_EXCLUSIVE);

        final CountDownLatch latch = new CountDownLatch(1);

        QueueConsumer = session.createFlow(new XMLMessageListener() {
            @Override
            public void onReceive(BytesXMLMessage msg) {
                // your implementation here...
                if (msg instanceof TextMessage) {
                    System.out.printf("TextMessage received: '%s'%n", ((TextMessage) msg).getText());
                } else {
                    System.out.println("Message received.");
                }
                System.out.printf("Message Dump:%n%s%n", msg.dump());
                // When the ack mode is set to SUPPORTED_MESSAGE_ACK_CLIENT,
                // guaranteed delivery messages are acknowledged after
                // processing
                msg.ackMessage();
                latch.countDown(); // unblock main thread
            }

            @Override
            public void onException(JCSMPException e) {
                System.out.printf("Consumer received exception: %s%n", e);
                latch.countDown(); // unblock main thread
            }
        }, flow_prop, endpoint_props);

        QueueConsumer.start();
    }

    public void stopReceivingFromQueue() {
        if (QueueConsumer != null) {
            QueueConsumer.stop();
        }
        if (session != null) {
            session.closeSession();
        }
    }
}
