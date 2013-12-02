package dk.dbc.dataio.sink.es.queue.rest;

import dk.dbc.dataio.sink.es.queue.ejb.EsBean;
import dk.dbc.dataio.sink.es.queue.ejb.EsInflightBean;
import dk.dbc.dataio.sink.es.queue.ejb.QueueBean;
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
public class EsQueueApplication extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(EsQueueApplication.class);

    @Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> classes = new HashSet<>();
        classes.add(EsBean.class);
        classes.add(EsInflightBean.class);
        classes.add(QueueBean.class);
        for (Class<?> clazz : classes) {
            LOGGER.info("Registered {} resource", clazz.getName());
        }
        return classes;
    }
}
