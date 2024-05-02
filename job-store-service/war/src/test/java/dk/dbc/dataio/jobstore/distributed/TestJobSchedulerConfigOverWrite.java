package dk.dbc.dataio.jobstore.distributed;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ja7 on 06-05-16.
 */
@Singleton
@Startup
public class TestJobSchedulerConfigOverWrite {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestJobSchedulerConfigOverWrite.class);

    @PostConstruct
    void do_startupQueueSizeOverWrite() {
        LOGGER.info("in do_startupQueueSizeOverWrite()");
        ChunkSchedulingStatus.QUEUED_FOR_PROCESSING.max = 10;
        ChunkSchedulingStatus.QUEUED_FOR_DELIVERY.max = 10;
        ChunkSchedulingStatus.transitionToDirectMark = 5;
    }
}
