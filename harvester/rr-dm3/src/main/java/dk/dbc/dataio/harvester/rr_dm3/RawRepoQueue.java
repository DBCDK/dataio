package dk.dbc.dataio.harvester.rr_dm3;

import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.RRV3HarvesterConfig;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepo3Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Abstraction layer for rawrepo queue.
 * This class is not thread safe.
 */
public class RawRepoQueue implements RecordHarvestTaskQueue {
    private static final Logger LOGGER = LoggerFactory.getLogger(RawRepoQueue.class);
    private static final int HIGH_PRIORITY_THRESHOLD = 500;

    private final RRV3HarvesterConfig.Content config;
    private final RawRepo3Connector rawRepoConnector;
    private final int pileUpDuration;
    private final ChronoUnit pileUpDurationUnit;
    private RawRepoRecordHarvestTask head;

    private boolean breakDequeueLoop = false;
    private Instant firstHighPrioritySeenAt = null;

    public RawRepoQueue(RRV3HarvesterConfig config, RawRepo3Connector rawRepoConnector) {
        this(config, rawRepoConnector, 45, ChronoUnit.SECONDS);
    }

    RawRepoQueue(RRV3HarvesterConfig config, RawRepo3Connector rawRepoConnector,
                 int pileUpDuration, ChronoUnit pileUpDurationUnit) {
        this.config = config.getContent();
        this.rawRepoConnector = rawRepoConnector;
        this.head = null;
        this.pileUpDuration = pileUpDuration;
        this.pileUpDurationUnit = pileUpDurationUnit;
    }

    /**
     * Retrieves, but does not remove, the head of this rawrepo queue, or returns null if this queue is empty.
     *
     * @return the head of this rawrepo queue, or null if this queue is empty
     * @throws HarvesterException on error while retrieving a queued record
     */
    @Override
    public RawRepoRecordHarvestTask peek() throws HarvesterException {
        return null;
    }

    /**
     * Retrieves and removes the head of this rawrepo queue, or returns null if this queue is empty.
     *
     * @return the head of this rawrepo queue, or null if this queue is empty
     * @throws HarvesterException on error while retrieving a queued record
     */
    @Override
    public RawRepoRecordHarvestTask poll() throws HarvesterException {
        return null;
    }

    @Override
    public int estimatedSize() {
        return head == null ? 0 : 1;
    }

    @Override
    public int basedOnJob() {
        return 0;
    }

    @Override
    public boolean isEmpty() throws HarvesterException {
        return peek() == null;
    }

    @Override
    public void commit() {
    }
}
