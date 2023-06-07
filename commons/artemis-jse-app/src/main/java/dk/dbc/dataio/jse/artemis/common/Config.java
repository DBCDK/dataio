package dk.dbc.dataio.jse.artemis.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.ConnectionFactory;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public enum Config implements EnvConfig {
    WEB_PORT("8080"),
    ARTEMIS_MQ_HOST,
    ARTEMIS_CONNECTION_FACTORY(qpidCF()),
    ARTEMIS_BROKER_PROTOCOL("amqp"),
    ARTEMIS_JMS_PORT("61616"),
    ARTEMIS_ADMIN_PORT,
    ARTEMIS_USER,
    ARTEMIS_PASSWORD,
    RECONNECT_DELAY("PT10s"),
    MESSAGE_FILTER,
    JOBSTORE_URL,
    STALE_JMS_PROVIDER("PT1m"),
    STALE_THRESHOLD("20"),
    CONSUMER_THREADS("1"),
    CONSUMER_IDLE_MAX("30");

    private final String defaultValue;
    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);
    private static final AtomicInteger THREAD_ID = new AtomicInteger(0);

    Config() {
        this(null);
    }

    Config(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public static String getBrokerUrl() {
        return ARTEMIS_BROKER_PROTOCOL + "://" + ARTEMIS_MQ_HOST + ":" + ARTEMIS_JMS_PORT;
    }

    public static String artemisCF() {
        return "org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory";
    }

    public static String qpidCF() {
        return "org.apache.qpid.jms.JmsConnectionFactory";
    }

    public static ConnectionFactory getConnectionFactory() {
        try {
            Class<?> cfClass = Class.forName(ARTEMIS_CONNECTION_FACTORY.asString());
            return (ConnectionFactory) cfClass.getConstructor(String.class).newInstance(getBrokerUrl());
        } catch (Exception e) {
            throw new Error("Unable to create connection factory using class: " + ARTEMIS_CONNECTION_FACTORY.asString() + ", and broker url: " + getBrokerUrl(), e);
        }
    }

    public static ThreadFactory threadFactory(String name, boolean daemon) {
        return r -> {
            Thread thread = new Thread(r, name + "-" + THREAD_ID.getAndIncrement());
            thread.setDaemon(daemon);
            thread.setUncaughtExceptionHandler((t, e) -> LOGGER.error("Exception handler caught exception in {}", t.getName(), e));
            return thread;
        };
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String toString() {
        return asString();
    }
}
