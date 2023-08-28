package dk.dbc.dataio.jobstore.service.ejb;

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
        JobSchedulerBean.MAX_NUMBER_OF_CHUNKS_IN_DELIVERING_QUEUE_PER_SINK = 10;
        JobSchedulerBean.MAX_NUMBER_OF_CHUNKS_IN_PROCESSING_QUEUE_PER_SINK = 10;
        JobSchedulerBean.TRANSITION_TO_DIRECT_MARK = 5;
    }
}
