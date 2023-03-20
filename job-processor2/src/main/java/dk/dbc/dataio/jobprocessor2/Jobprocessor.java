package dk.dbc.dataio.jobprocessor2;

import dk.dbc.dataio.jobprocessor2.jms.JobStoreMessageConsumer;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.IllegalStateRuntimeException;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static dk.dbc.dataio.jobprocessor2.Config.ARTEMIS_HOST;
import static dk.dbc.dataio.jobprocessor2.Config.ARTEMIS_JMS_PORT;

public class Jobprocessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(Jobprocessor.class);
    private static final AtomicBoolean keepRunning = new AtomicBoolean(true);
    private String brokerUrl = "tcp://" + ARTEMIS_HOST + ":" + ARTEMIS_JMS_PORT;
    private static final Duration RECONNECT_DELAY = Duration.parse(Config.RECONNECT_DELAY.toString());
    ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
    MessageListener listener;



    public static void main(String[] args) {
        new Jobprocessor().go();
    }

    private void go() {
        try (ServiceHub serviceHub = new ServiceHub.Builder().build()) {
            listener = new JobStoreMessageConsumer(serviceHub);
            listen();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void stop() {
        keepRunning.set(false);
    }

    public void listen() {
        while (keepRunning.get()) {
            try (JMSContext context = connectionFactory.createContext(JMSContext.SESSION_TRANSACTED)) {
                Queue queue = context.createQueue(Config.QUEUE.toString());
                try(JMSConsumer consumer = context.createConsumer(queue, Config.MESSAGE_FILTER.asOptionalString().orElse(null))) {
                    receiveMessages(consumer, context);
                }
            } catch (JMSRuntimeException e) {
                LOGGER.error("Failed to connect to artemis on {} retrying in {}s, cause: {}", brokerUrl, RECONNECT_DELAY.toSeconds(), e.getCause().getMessage());
                sleep(RECONNECT_DELAY);
            }
        }
    }

    private void receiveMessages(JMSConsumer consumer, JMSContext context) {
        String messageId = null;
        try {
            while(keepRunning.get()) {
                Message message = consumer.receive();
                if(message != null) {
                    messageId = message.getJMSMessageID();
                    listener.onMessage(message);
                    context.commit();
                }
            }
        } catch (IllegalStateRuntimeException ie) {
            LOGGER.info("Artemis connection broke, reconnecting", ie);
            sleep(Duration.ofSeconds(1));
        } catch (Throwable t) {
            if(t instanceof Error) {
                LOGGER.error("Message loop caught a critical error, shutting down!", t);
                keepRunning.set(false);
                return;
            }
            LOGGER.warn("Rolling back message {}", messageId, t);
            context.rollback();
        }
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
