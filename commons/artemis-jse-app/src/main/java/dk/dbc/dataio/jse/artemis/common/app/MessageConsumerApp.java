package dk.dbc.dataio.jse.artemis.common.app;

import dk.dbc.dataio.commons.types.Tools;
import dk.dbc.dataio.jse.artemis.common.Config;
import dk.dbc.dataio.jse.artemis.common.Metric;
import dk.dbc.dataio.jse.artemis.common.jms.MessageConsumer;
import dk.dbc.dataio.jse.artemis.common.service.HttpService;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.IllegalStateRuntimeException;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSRuntimeException;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public abstract class MessageConsumerApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumerApp.class);
    private static final AtomicBoolean keepRunning = new AtomicBoolean(true);
    private static final Duration RECONNECT_DELAY = Duration.parse(Config.RECONNECT_DELAY.toString());
    private final ConnectionFactory connectionFactory = Config.getConnectionFactory();
    private final ExecutorService executorService = Executors.newFixedThreadPool(Config.CONSUMER_THREADS.asInteger(), Config.threadFactory("message-consumer", false));
    private HttpService httpService = null;
    private Supplier<? extends MessageConsumer> messageConsumerSupplier = null;
    private static final int CONSUMER_IDLE_MAX = Config.CONSUMER_IDLE_MAX.asInteger();
    private static final AtomicBoolean LOG_QUEUE_CONFIG = new AtomicBoolean(true);
    private static final ConcurrentHashMap<String, Instant> TX_ELAPSED = new ConcurrentHashMap<>();

    protected void go(ServiceHub serviceHub, Supplier<? extends MessageConsumer> messageConsumerSupplier) {
        int threads = Config.CONSUMER_THREADS.asInteger();
        httpService = serviceHub.httpService;
        httpService.start();
        this.messageConsumerSupplier = messageConsumerSupplier;
        for (int i = 0; i < threads; i++) {
            startConsumers();
        }
        Metric.dataio_tx_elapsed.gauge(this::getLongestRunningTX);
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    private long getLongestRunningTX() {
        Instant now = Instant.now();
        return TX_ELAPSED.values().stream().map(i -> Duration.between(i, now)).mapToLong(Duration::toMillis).max().orElse(0);
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
                MessageConsumer messageConsumer = messageConsumerSupplier.get();
                Queue queue = context.createQueue(messageConsumer.getFQN());
                if(LOG_QUEUE_CONFIG.get()) {
                    LOGGER.info("Setting up queue listener for {} with filter '{}' using connection factory {}", queue, messageConsumer.getFilter(), connectionFactory.getClass().getName());
                    LOG_QUEUE_CONFIG.set(false);
                }
                try(JMSConsumer consumer = context.createConsumer(queue, messageConsumer.getFilter())) {
                    receiveMessages(messageConsumer, consumer, context);
                }
            } catch (JMSRuntimeException e) {
                LOGGER.error("Failed to connect to artemis on {} retrying in {}s", Config.getBrokerUrl(), RECONNECT_DELAY.toSeconds(), e);
                sleep(RECONNECT_DELAY);
            } catch (RuntimeException e) {
                LOGGER.error("Caught generic exception in listener loop", e);
                sleep(RECONNECT_DELAY);
            }
        }
    }

    private void receiveMessages(MessageConsumer listener, JMSConsumer consumer, JMSContext context) {
        String messageId = null;
        try {
            int noMsgCount = 0;
            while(keepRunning.get() && (noMsgCount < CONSUMER_IDLE_MAX || CONSUMER_IDLE_MAX == -1)) {
                Message message = consumer.receive(1000);
                if(message != null) {
                    noMsgCount = 0;
                    messageId = message.getJMSMessageID() == null ? UUID.randomUUID().toString() : message.getJMSMessageID();
                    TX_ELAPSED.put(messageId, Instant.now());
                    try {
                        listener.onMessage(message);
                        context.commit();
                    } catch (RuntimeException re) {
                        LOGGER.warn("Rolling back message {}", messageId, re);
                        context.rollback();
                    } finally {
                        TX_ELAPSED.remove(messageId);
                    }
                } else noMsgCount++;
            }
        } catch (IllegalStateRuntimeException ie) {
            LOGGER.info("Artemis connection broke, reconnecting", ie);
            Tools.sleep(1000);
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
}
