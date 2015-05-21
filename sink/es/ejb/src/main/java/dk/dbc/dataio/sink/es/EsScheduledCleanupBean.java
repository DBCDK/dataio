package dk.dbc.dataio.sink.es;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;

@LocalBean
@Singleton
@Startup
public class EsScheduledCleanupBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(EsScheduledCleanupBean.class);

    @EJB
    EsCleanupBean esCleanupBean;

    @Schedule(second = "*/15", minute = "*", hour = "*", persistent = false)
    public void cleanup() {
        try {
            esCleanupBean.cleanup();
        } catch (Exception e) {
            LOGGER.error("Exception caught from ES cleanup", e);
        }
    }
}
