package dk.dbc.dataio.jobstore.service.ejb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * Created by sma on 13-06-17.
 * This enterprise Java bean represents periodic attempts at purging old jobs and all relating entries from the system.
 */

@Singleton
@Startup
public class ScheduledJobPurgeBean {

    @EJB JobPurgeBean jobPurgeBean;

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledJobPurgeBean.class);

    @Schedule(hour="23", minute = "*", second = "*", persistent = false)
    public void run() {
        try {
            jobPurgeBean.purgeJobs();
        } catch (Exception e) {
            LOGGER.error("Exception caught during scheduled job purge", e);
        }
    }
}
