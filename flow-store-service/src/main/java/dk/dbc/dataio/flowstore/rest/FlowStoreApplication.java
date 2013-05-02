package dk.dbc.dataio.flowstore.rest;

import dk.dbc.dataio.flowstore.ejb.FlowsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * This class defines the other classes that make up this JAX-RS application
 * by having the getClasses method return a specific set of resources.
 */
@ApplicationPath("/")
public class FlowStoreApplication extends Application {
    private static final Logger log = LoggerFactory.getLogger(FlowStoreApplication.class);

    @Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> classes = new HashSet<>();
        log.debug("Registering {} resource", FlowsBean.class.getName());
        classes.add(FlowsBean.class);
        classes.add(PersistenceExceptionMapper.class);
        return classes;
    }
}
