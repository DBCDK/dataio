package dk.dbc.dataio.jse.artemis.common.app;

import dk.dbc.dataio.jse.artemis.common.Config;
import dk.dbc.dataio.jse.artemis.common.jms.MessageConsumer;
import dk.dbc.dataio.jse.artemis.common.service.HttpService;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.ConnectionFactory;
import javax.jms.IllegalStateRuntimeException;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;
import javax.jms.Queue;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class MessageConsumerApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumerApp.class);
    private static final AtomicBoolean keepRunning = new AtomicBoolean(true);
    private static final Duration RECONNECT_DELAY = Duration.parse(Config.RECONNECT_DELAY.toString());
    private final ConnectionFactory connectionFactory = Config.getConnectionFactory();
    private final ExecutorService executorService = Executors.newFixedThreadPool(Config.CONSUMER_THREADS.asInteger(), Config.threadFactory("message-consumer", false));
    private HttpService httpService = null;
    private MessageConsumer messageConsumer = null;

    protected void go(ServiceHub serviceHub, MessageConsumer messageConsumer) {
        int threads = Config.CONSUMER_THREADS.asInteger();
        httpService = serviceHub.httpService;
        this.messageConsumer = messageConsumer;
        for (int i = 0; i < threads; i++) {
            startConsumers();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public void startConsumers() {
        executorService.submit(this::listen);
    }

    public void stop() {
        keepRunning.set(false);
        executorService.shutdown();
        try {
            if(httpService != null) httpService.close();
        } catch (Exception ignored) {
        }
    }

    private void listen() {
        while (keepRunning.get()) {
            try (JMSContext context = connectionFactory.createContext(JMSContext.SESSION_TRANSACTED)) {
                Queue queue = context.createQueue(messageConsumer.getQueue());
                try(JMSConsumer consumer = context.createConsumer(queue, messageConsumer.getFilter())) {
                    receiveMessages(messageConsumer, consumer, context);
                }
            } catch (JMSRuntimeException e) {
                LOGGER.error("Failed to connect to artemis on {} retrying in {}s, cause: {}", Config.getBrokerUrl(), RECONNECT_DELAY.toSeconds(), e.getCause().getMessage());
                sleep(RECONNECT_DELAY);
            }
        }
    }

    private void receiveMessages(MessageConsumer listener, JMSConsumer consumer, JMSContext context) {
        String messageId = null;
        try {
            while(keepRunning.get()) {
                Message message = consumer.receive(1000);
                if(message != null) {
                    messageId = message.getJMSMessageID();
                    try {
                        listener.onMessage(message);
                        context.commit();
                    } catch (RuntimeException re) {
                        LOGGER.warn("Rolling back message {}", messageId, re);
                        context.rollback();
                    }
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
