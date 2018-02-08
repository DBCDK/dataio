package dk.dbc.dataio.jobrerunrules.service.rest;

import dk.dbc.dataio.jobrerunrules.service.JobRerunRulesBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * This class defines the other classes that make up this JAX-RS application by
 * having the getClasses method return a specific set of resources.
 */
@ApplicationPath("/")
public class JobRerunRules extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(
        JobRerunRules.class);

    @Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> classes = new HashSet<>();
        classes.add(JobRerunRulesBean.class);
        classes.add(StatusBean.class);
        for (Class<?> clazz : classes) {
            LOGGER.info("Registered {} resource", clazz.getName());
        }
        return classes;
    }
}
