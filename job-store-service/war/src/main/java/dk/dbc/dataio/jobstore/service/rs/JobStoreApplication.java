package dk.dbc.dataio.jobstore.service.rs;

import dk.dbc.dataio.jobstore.service.ejb.JobSchedulerRestBean;
import dk.dbc.dataio.jobstore.service.ejb.JobsBean;
import dk.dbc.dataio.jobstore.service.ejb.JobsExportsBean;
import dk.dbc.dataio.jobstore.service.ejb.NotificationsBean;
import dk.dbc.dataio.jobstore.service.ejb.RerunsBean;
import dk.dbc.dataio.jobstore.service.ejb.SinkMessageProducerBean;
import dk.dbc.dataio.jobstore.service.ejb.developer.Developer;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class defines the other classes that make up this JAX-RS application by
 * having the getClasses method return a specific set of resources.
 */
@ApplicationPath("/")
public class JobStoreApplication extends Application {
    private final static Logger LOGGER = LoggerFactory.getLogger(JobStoreApplication.class);
    private static final Set<Class<?>> classes = makeClasses();

    private static Set<Class<?>> makeClasses() {
        Stream<Class<?>> stream = Stream.of(JobsBean.class, JobsExportsBean.class, NotificationsBean.class, StatusBean.class, JobSchedulerRestBean.class,
                RerunsBean.class, SinkMessageProducerBean.class, AdminBean.class);

        if("on".equals(System.getenv("DEVELOPER"))) {
            LOGGER.info("DEVELOPER signalled. Adding developer bean to pool of beans in JobstoreApplication");
            return Stream.concat(stream, Stream.of(Developer.class)).collect(Collectors.toSet());
        }
        return stream.collect(Collectors.toSet());
    }

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }
}
