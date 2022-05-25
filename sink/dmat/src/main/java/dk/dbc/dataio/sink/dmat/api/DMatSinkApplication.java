package dk.dbc.dataio.sink.dmat.api;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * This class defines the other classes that make up this JAX-RS application by
 * having the getClasses method return a specific set of resources.
 */
@ApplicationPath("/")
public class DMatSinkApplication extends Application {
    private static final Set<Class<?>> classes = new HashSet<>();

    static {
        classes.add(StatusBean.class);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }
}
