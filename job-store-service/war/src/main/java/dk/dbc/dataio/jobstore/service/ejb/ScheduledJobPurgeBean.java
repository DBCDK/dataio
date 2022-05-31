package dk.dbc.dataio.jobstore.service.ejb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * This enterprise Java bean represents periodic attempts at purging old jobs and all related entries from the system.
 */
@Singleton
@Startup
public class ScheduledJobPurgeBean {

    @EJB
    JobPurgeBean jobPurgeBean;

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledJobPurgeBean.class);

    @Schedule(hour = "23", persistent = false)
    public void run() {
        try {
            jobPurgeBean.purgeJobs();
        } catch (Exception e) {
            LOGGER.error("Exception caught during scheduled job purge", e);
        }
    }
}
