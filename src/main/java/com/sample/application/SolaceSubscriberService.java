package com.sample.application;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.ConsumerFlowProperties;
import com.solacesystems.jcsmp.EndpointProperties;
import com.solacesystems.jcsmp.FlowReceiver;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishEventHandler;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageListener;
import com.solacesystems.jcsmp.XMLMessageProducer;

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

    @Value("${solace.ssl.trust-store-path}")
    private String trustStorePath;

    @Value("${solace.ssl.trust-store-password}")
    private String trustStorePassword;

    @Value("${solace.ssl.ssl-validate:true}")
    private boolean sslValidate;

    private JCSMPSession session;
    private XMLMessageConsumer consumer;
    private FlowReceiver QueueConsumer;
    private AtomicBoolean schedulerActive = new AtomicBoolean(false);

    public void connect() {
        try {
            // Create the connection factory
            JCSMPProperties properties = new JCSMPProperties();
            properties.setProperty(JCSMPProperties.HOST, host);
            properties.setProperty(JCSMPProperties.USERNAME, username);
            properties.setProperty(JCSMPProperties.PASSWORD, password);
            properties.setProperty(JCSMPProperties.VPN_NAME, vpn);
            properties.setProperty(JCSMPProperties.SSL_TRUST_STORE, trustStorePath);
            properties.setProperty(JCSMPProperties.SSL_TRUST_STORE_PASSWORD, trustStorePassword);
            properties.setBooleanProperty(JCSMPProperties.SSL_VALIDATE_CERTIFICATE, sslValidate);

            // Create a session
            session = JCSMPFactory.onlyInstance().createSession(properties);
            session.connect();

        } catch (JCSMPException e) {
            e.printStackTrace();
        }
    }

    public void startScheduler() {
        schedulerActive.set(true);
    }

    @Scheduled(fixedRate = 10000) // Run every 10 seconds
    public void scheduledTask() {
        System.out.println("scheduledTask");
        if (schedulerActive.get()) {
            startSendMessageToTopic(); // Trigger the message sending
        }
    }

    public void startSendMessageToTopic() {
        try {
            sendMessage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessage() {
        try {
            XMLMessageProducer producer = session.getMessageProducer(new JCSMPStreamingPublishEventHandler() {

                @Override
                public void responseReceived(String messageID) {
                    System.out.println("Producer received response for msg: " + messageID);
                }

                @Override
                public void handleError(String messageID, JCSMPException e, long timestamp) {
                    System.out.printf("Producer received error for msg: %s@%s - %s%n",
                            messageID, timestamp, e);
                }
            });

            TextMessage message = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
            message.setText("Hello, Solace! This is a triggered message.");
            producer.send(message, JCSMPFactory.onlyInstance().createTopic(topic));
            System.out.println("Message sent to topic");
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
                                ((TextMessage) msg).getText());
                    } else {
                        System.out.println("Message received.");
                    }
                    System.out.printf("Message Dump:%n%s%n", msg.dump());
                    latch.countDown(); // unblock main thread
                }

                @Override
                public void onException(JCSMPException e) {
                    System.out.printf("Consumer received exception: %s%n", e);
                    latch.countDown(); // unblock main thread
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
