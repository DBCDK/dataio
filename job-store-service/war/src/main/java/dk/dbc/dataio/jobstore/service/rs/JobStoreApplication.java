package dk.dbc.dataio.jobstore.service.rs;

import dk.dbc.dataio.jobstore.service.ejb.JobSchedulerRestBean;
import dk.dbc.dataio.jobstore.service.ejb.JobsBean;
import dk.dbc.dataio.jobstore.service.ejb.JobsExportsBean;
import dk.dbc.dataio.jobstore.service.ejb.NotificationsBean;
import dk.dbc.dataio.jobstore.service.ejb.RerunsBean;
import dk.dbc.dataio.jobstore.service.ejb.SinkMessageProducerBean;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Set;

/**
 * This class defines the other classes that make up this JAX-RS application by
 * having the getClasses method return a specific set of resources.
 */
@ApplicationPath("/")
public class JobStoreApplication extends Application {
    private static final Set<Class<?>> classes = Set.of(JobsBean.class, JobsExportsBean.class, NotificationsBean.class, StatusBean.class, JobSchedulerRestBean.class,
            RerunsBean.class, SinkMessageProducerBean.class);

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }
}
