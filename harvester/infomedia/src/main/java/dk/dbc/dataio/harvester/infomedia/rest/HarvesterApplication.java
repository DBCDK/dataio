package dk.dbc.dataio.harvester.infomedia.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import java.util.HashSet;
import java.util.Set;

/**
 * This class defines the other classes that make up this JAX-RS application by
 * having the getClasses method return a specific set of resources.
 */
@ApplicationPath("/")
public class HarvesterApplication extends Application {
    private static final Set<Class<?>> CLASSES = new HashSet<>();

    static {
        CLASSES.add(StatusBean.class);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return CLASSES;
    }
}
