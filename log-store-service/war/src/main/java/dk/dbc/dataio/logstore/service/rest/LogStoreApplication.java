package dk.dbc.dataio.logstore.service.rest;

import dk.dbc.dataio.logstore.service.ejb.LogEntriesBean;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import java.util.Set;

/**
 * This class defines the other classes that make up this JAX-RS application by
 * having the getClasses method return a specific set of resources.
 */
@ApplicationPath("/")
public class LogStoreApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(LogEntriesBean.class, StatusBean.class);
    }
}
