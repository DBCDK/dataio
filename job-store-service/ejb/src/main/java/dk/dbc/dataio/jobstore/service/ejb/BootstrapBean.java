package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;

@Singleton
@Startup
@DependsOn("StartupDBMigrator")
public class BootstrapBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapBean.class);

    @EJB
    PgJobStore jobStore;

    @EJB
    JobSchedulerBean jobSchedulerBean;

    @PostConstruct
    public void initialize() {
        final StopWatch stopWatch = new StopWatch();
        try {
            restoreSystemState();
        } finally {
            LOGGER.debug("job-store initialization took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    @Lock(LockType.READ)
    public void waitForSystemInitialization() {
        LOGGER.debug("job-store initialized");
    }

    private void restoreSystemState() {
        LOGGER.info("Restoring job-store state");
    }
}
