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
public class ScheduledOpenAgencyConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledOpenAgencyConnectorBean.class);

    @EJB
    OpenAgencyConnectorBean openAgencyConnectorBean;

    // to ensure thread safety
    private final ConcurrentHashMap<Integer, Boolean> phLibraryMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Boolean> worldcatLibraryMap =
        new ConcurrentHashMap<>();

    @Schedule(second = "*", minute = "*/5", hour = "*", persistent = false)
    public void resetMap() {
        try {
            setPhLibraryMap();
            setWoldcatLibraries();
        } catch(Exception e) {
            LOGGER.error("Caught exception", e);
        }
    }

    private void setPhLibraryMap() throws OpenAgencyConnectorException {
        Set<Integer> phLibraries = openAgencyConnectorBean.getConnector()
            .getPHLibraries();
        phLibraryMap.clear();
        phLibraries.forEach(submitter -> phLibraryMap.put(submitter, true));
    }

    private void setWoldcatLibraries() throws OpenAgencyConnectorException {
        Set<Integer> worldcatLibraries = openAgencyConnectorBean.getConnector()
            .getWorldCatLibraries();
        worldcatLibraryMap.clear();
        worldcatLibraries.forEach(submitter -> worldcatLibraryMap.put(
            submitter, true));
    }

    public Set<Integer> getPhLibraries() throws OpenAgencyConnectorException {
        if(phLibraryMap.isEmpty())
            setPhLibraryMap();
        return new HashSet<>(phLibraryMap.keySet());
    }

    public Set<Integer> getWorldCatLibraries() throws OpenAgencyConnectorException {
        if(worldcatLibraryMap.isEmpty()) setWoldcatLibraries();
        return new HashSet<>(worldcatLibraryMap.keySet());
    }
}