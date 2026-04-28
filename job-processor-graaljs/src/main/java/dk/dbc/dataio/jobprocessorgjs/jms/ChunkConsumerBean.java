package dk.dbc.dataio.jobprocessorgjs.jms;

import dk.dbc.commons.useragent.UserAgent;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.jobprocessorgjs.health.ProcessorHealth;
import dk.dbc.dataio.jobprocessorgjs.service.ChunkProcessor;
import dk.dbc.dataio.jobprocessorgjs.service.FlowCache;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSRuntimeException;
import jakarta.jms.Message;
import jakarta.ws.rs.client.ClientBuilder;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
@Startup
public class ChunkConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkConsumerBean.class);

    @Inject @ConfigProperty(name = "ARTEMIS_MQ_HOST")
    private String artemisHost;

    @Inject @ConfigProperty(name = "ARTEMIS_JMS_PORT", defaultValue = "61616")
    private String artemisPort;

    @Inject @ConfigProperty(name = "ARTEMIS_USER")
    private String artemisUser;

    @Inject @ConfigProperty(name = "ARTEMIS_PASSWORD")
    private String artemisPassword;

    @Inject @ConfigProperty(name = "QUEUE")
    private String queue;

    @Inject @ConfigProperty(name = "JOBSTORE_URL")
    private String jobstoreUrl;

    @Inject @ConfigProperty(name = "FLOWSTORE_URL", defaultValue = "")
    private String flowstoreUrl;

    @Inject @ConfigProperty(name = "FLOW_CACHE_SIZE", defaultValue = "100")
    private int flowCacheSize;

    @Inject @ConfigProperty(name = "FLOW_CACHE_EXPIRY", defaultValue = "PT10m")
    private String flowCacheExpiry;

    @Inject @ConfigProperty(name = "CONSUMER_THREADS", defaultValue = "1")
    private int consumerThreads;

    @Inject
    private ProcessorHealth health;

    private List<ChunkMessageConsumer> messageConsumers = List.of();
    private ConnectionFactory connectionFactory;
    private ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @PostConstruct
    void start() {
        connectionFactory = new ActiveMQConnectionFactory(
                "tcp://" + artemisHost + ":" + artemisPort, artemisUser, artemisPassword);

        JobStoreServiceConnector jobStoreConnector = new JobStoreServiceConnector(
                ClientBuilder.newClient().register(new JacksonFeature()),
                UserAgent.forInternalRequests(), jobstoreUrl);

        FlowStoreServiceConnector flowStoreConnector = flowstoreUrl.isBlank() ? null
                : new FlowStoreServiceConnector(
                        ClientBuilder.newClient().register(new JacksonFeature()),
                        UserAgent.forInternalRequests(), flowstoreUrl);

        List<ChunkMessageConsumer> consumers = new ArrayList<>(consumerThreads);
        running.set(true);
        executor = Executors.newFixedThreadPool(consumerThreads,
                Thread.ofPlatform().name("graaljs-consumer-", 0).factory());
        for (int i = 0; i < consumerThreads; i++) {
            FlowCache flowCache = new FlowCache(flowCacheSize, Duration.parse(flowCacheExpiry));
            ChunkProcessor chunkProcessor = new ChunkProcessor(
                    health, flowCache,
                    jobId -> getFlow(jobId, jobStoreConnector, flowStoreConnector));
            ChunkMessageConsumer consumer = new ChunkMessageConsumer(chunkProcessor, jobStoreConnector);
            consumers.add(consumer);
            executor.submit(() -> listen(consumer));
        }
        messageConsumers = List.copyOf(consumers);
        LOGGER.info("Started {} GraalJS chunk consumer thread(s) on queue {}", consumerThreads, queue);
    }

    @PreDestroy
    void stop() {
        running.set(false);
        executor.shutdownNow();
    }

    @Schedule(hour = "*", minute = "*", second = "*/30", persistent = false)
    void checkTimeouts() {
        messageConsumers.forEach(c -> c.checkTimeouts(health));
    }

    private void listen(ChunkMessageConsumer messageConsumer) {
        String fqn = queue.contains("::") ? queue : queue + "::" + queue;
        try {
            while (running.get()) {
                try (JMSContext context = connectionFactory.createContext(JMSContext.SESSION_TRANSACTED)) {
                    try (JMSConsumer consumer = context.createConsumer(context.createQueue(fqn))) {
                        receiveMessages(context, consumer, messageConsumer);
                    }
                } catch (JMSRuntimeException e) {
                    LOGGER.error("JMS connection failed, retrying in 10s", e);
                    sleep(10_000);
                } catch (RuntimeException e) {
                    LOGGER.error("Unexpected error in consumer loop, retrying in 10s", e);
                    sleep(10_000);
                }
            }
        } catch (Throwable t) {
            // An unrecoverable error (e.g. OutOfMemoryError) escaped the reconnect loop.
            // Signal liveness failure so the liveness probe triggers a pod restart.
            LOGGER.error("Consumer thread terminated by unrecoverable error — signalling liveness failure", t);
            health.signalFatal(t.getClass().getSimpleName() + ": " + t.getMessage());
            throw t;
        }
    }

    private void receiveMessages(JMSContext context, JMSConsumer consumer,
                                 ChunkMessageConsumer messageConsumer) {
        while (running.get()) {
            Message message = consumer.receive(1000);
            if (message == null) continue;
            try {
                messageConsumer.onMessage(message);
                context.commit();
            } catch (Exception e) {
                LOGGER.warn("Rolling back message due to processing error", e);
                context.rollback();
            }
        }
    }

    private Flow getFlow(int jobId, JobStoreServiceConnector jobStoreConnector,
                         FlowStoreServiceConnector flowStoreConnector) throws Exception {
        Flow flow = jobStoreConnector.getCachedFlow(jobId);
        if (flow.getContent().getJsar() == null && flowStoreConnector != null) {
            byte[] jsar = flowStoreConnector.getJsar(flow.getId());
            return new Flow(flow.getId(), flow.getVersion(),
                    new FlowContent(jsar, flow.getContent().getTimeOfLastModification()));
        }
        return flow;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
