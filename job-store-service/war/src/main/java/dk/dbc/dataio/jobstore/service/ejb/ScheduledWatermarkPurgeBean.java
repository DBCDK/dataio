package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.service.dependencytracking.Hazelcast;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This enterprise Java bean represents periodic attempts at purging stale
 * sink_record_delivery_watermark rows from the system.
 */
@Singleton
@Startup
public class ScheduledWatermarkPurgeBean {

    @EJB
    WatermarkPurgeBean watermarkPurgeBean;

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledWatermarkPurgeBean.class);

    @Schedule(hour = "3", persistent = false)
    public void run() {
        try {
            if (Hazelcast.isSlave()) {
                return;
            }
            watermarkPurgeBean.purgeStaleWatermarks();
        } catch (Exception e) {
            LOGGER.error("Exception caught during scheduled watermark purge", e);
        }
    }
}
