package dk.dbc.dataio.jobstore.service.ejb;

import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This enterprise Java bean represents periodic attempts at purging old jobs and all related entries from the system.
 */
@Singleton
@Startup
public class ScheduledJobPurgeBean {

    @EJB
    JobPurgeBean jobPurgeBean;

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledJobPurgeBean.class);

    @Schedule(hour = "23")
    public void run() {
        try {
            jobPurgeBean.purgeJobs();
        } catch (Exception e) {
            LOGGER.error("Exception caught during scheduled job purge", e);
        }
    }
}
