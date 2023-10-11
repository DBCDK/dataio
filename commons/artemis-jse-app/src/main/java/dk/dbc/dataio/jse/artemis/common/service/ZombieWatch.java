package dk.dbc.dataio.jse.artemis.common.service;

import dk.dbc.dataio.jse.artemis.common.Config;
import dk.dbc.dataio.jse.artemis.common.HealthFlag;
import dk.dbc.jms.artemis.AdminClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static dk.dbc.dataio.jse.artemis.common.Config.ARTEMIS_ADMIN_PORT;
import static dk.dbc.dataio.jse.artemis.common.Config.ARTEMIS_MQ_HOST;
import static dk.dbc.dataio.jse.artemis.common.Config.ARTEMIS_PASSWORD;
import static dk.dbc.dataio.jse.artemis.common.Config.ARTEMIS_USER;

public class ZombieWatch {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZombieWatch.class);
    private final AdminClient adminClient;
    private final HealthService healthService;
    private final Map<QueueKey, AtomicLong> monitors = new HashMap<>();
    private final Map<String, Runnable> checks = new ConcurrentHashMap<>();

    public ZombieWatch(HealthService healthService) {
        this.healthService = healthService;
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1, Config.threadFactory("zombie-watch", true));
        scheduledExecutorService.scheduleAtFixedRate(this::zombieWatch, 1, 1, TimeUnit.MINUTES);
        adminClient = ARTEMIS_MQ_HOST.asOptionalString().flatMap(host -> ARTEMIS_ADMIN_PORT.asOptionalInteger()
                .map(port -> new AdminClient("http://" + host + ":" + port, ARTEMIS_USER.toString(), ARTEMIS_PASSWORD.toString()))).orElse(null);
    }

    @SuppressWarnings("unused")
    public void addCheck(String name, Runnable runnable) {
        checks.putIfAbsent(name, runnable);
    }

    public void update(String address, String queue, String filter) {
        QueueKey queueKey = new QueueKey(address, queue, filter);
        AtomicLong ts = monitors.computeIfAbsent(queueKey, qk -> new AtomicLong());
        ts.set(System.currentTimeMillis());
    }

    private void zombieWatch() {
        monitors.forEach(this::checkMonitor);
        checks.values().forEach(Runnable::run);
    }

    private void checkMonitor(QueueKey queueKey, AtomicLong timestamp) {
        Duration sinceLastRun = Duration.ofMillis(getTimeSinceLastMessage(timestamp.get()));
        LOGGER.info("Time since last message: {}", sinceLastRun);
        if(adminClient != null && sinceLastRun.compareTo(Config.STALE_JMS_PROVIDER.asDuration()) >= 0) {
            int count = getMessageCount(queueKey);
            LOGGER.info("Messages on queue: {}, with filter: {}", count, queueKey.getFilter().orElse("<none>"));
            if(count > Config.STALE_THRESHOLD.asInteger()) {
                LOGGER.error("MessageBean has gone stale, marking the server down");
                healthService.signal(HealthFlag.STALE);
            }
        }
    }

    private int getMessageCount(QueueKey queueKey) {
        try {
            return queueKey.getFilter().map(ms -> adminClient.countMessages(queueKey.queue, queueKey.address, ms)).orElse(adminClient.getQueueAttribute(queueKey.queue, queueKey.address, "MessageCount"));
        } catch (Exception e) {
            LOGGER.warn("Unable to retrieve message count for queue {}", queueKey, e);
            return 0;
        }
    }

    private long getTimeSinceLastMessage(long timestamp) {
        return System.currentTimeMillis() - timestamp;
    }

    public static class QueueKey {
        public final String address;
        public final String queue;
        private final String filter;

        public QueueKey(String address, String queue, String filter) {
            this.address = address;
            this.queue = queue;
            this.filter = filter;
        }

        public Optional<String> getFilter() {
            return Optional.ofNullable(filter);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QueueKey queueKey = (QueueKey) o;
            return address.equals(queueKey.address) && queue.equals(queueKey.queue) && Objects.equals(filter, queueKey.filter);
        }

        @Override
        public int hashCode() {
            return Objects.hash(address, queue, filter);
        }

        @Override
        public String toString() {
            return "QueueKey{" + "address='" + address + '\'' + ", queue='" + queue + '\'' + ", filter='" + filter + '\'' + '}';
        }
    }
}
