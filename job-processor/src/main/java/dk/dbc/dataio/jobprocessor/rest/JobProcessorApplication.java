package dk.dbc.dataio.jobprocessor.rest;

import dk.dbc.dataio.jobprocessor.ejb.JMSConnectionDetach;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Set;

/**
 * This class defines the other classes that make up this JAX-RS application by
 * having the getClasses method return a specific set of resources.
 */
@ApplicationPath("/")
public class JobProcessorApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(StatusBean.class, JMSConnectionDetach.class);
    }
}
