package dk.dbc.dataio.jobprocessor.rest;

import org.apache.activemq.artemis.api.core.client.ActiveMQClient;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * This class defines the other classes that make up this JAX-RS application by
 * having the getClasses method return a specific set of resources.
 */
@ApplicationPath("/")
public class JobProcessorApplication extends Application {

    public JobProcessorApplication() {
        ((ThreadPoolExecutor) ActiveMQClient.getGlobalThreadPool()).setMaximumPoolSize(4);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return new HashSet<>(Collections.singletonList(StatusBean.class));
    }
}
