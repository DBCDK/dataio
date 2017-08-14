package dk.dbc.dataio.openagency.ejb;

import dk.dbc.dataio.openagency.OpenAgencyConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@Startup
public class ScheduledAgencyConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledAgencyConnectorBean.class);

    @EJB
    OpenAgencyConnectorBean openAgencyConnectorBean;

    private final ConcurrentHashMap<Integer, Boolean> phLibraryMap = new ConcurrentHashMap<>();

    @Schedule(second = "*", minute = "*/5", hour = "*", persistent = false)
    public void resetMap() {
        try {
            setPhLibraryMap();
        } catch(Exception e) {
            LOGGER.error("Caught exception", e);
        }
    }

    private void setPhLibraryMap() throws OpenAgencyConnectorException {
        Set<Integer> phLibraries = openAgencyConnectorBean.getConnector().getPHLibraries();
        phLibraryMap.clear();
        phLibraries.forEach(submitter -> phLibraryMap.put(submitter, true));
    }

    public Set<Integer> getPhLibraries() throws OpenAgencyConnectorException {
        if(phLibraryMap.isEmpty())
            setPhLibraryMap();
        return new HashSet<>(phLibraryMap.keySet());
    }
}
