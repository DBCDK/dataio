package dk.dbc.dataio.flowstore.rest;

import dk.dbc.dataio.flowstore.ejb.FlowComponentsBean;
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
        classes.add(FlowsBean.class);
        classes.add(FlowComponentsBean.class);
        for (Class<?> clazz : classes) {
            log.debug("Registered {} resource", clazz.getName());
        }
        classes.add(PersistenceExceptionMapper.class);
        return classes;
    }

    // Hardening: JAX-RS/Jackson integration stopped working during glassfish3 to glassfish4 migration.
    /*
    @Override
    public Set<Object> getSingletons() {
        HashSet<Object> singletons = new HashSet<>();
        // use Jackson to do JSON serialization with JAX-RS
        singletons.add(new org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider());
        return singletons;
    }
    */
}
