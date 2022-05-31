package dk.dbc.dataio.jobstore.service.rs;

import dk.dbc.dataio.jobstore.service.ejb.JobSchedulerRestBean;
import dk.dbc.dataio.jobstore.service.ejb.JobsBean;
import dk.dbc.dataio.jobstore.service.ejb.JobsExportsBean;
import dk.dbc.dataio.jobstore.service.ejb.NotificationsBean;
import dk.dbc.dataio.jobstore.service.ejb.RerunsBean;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * This class defines the other classes that make up this JAX-RS application by
 * having the getClasses method return a specific set of resources.
 */
@ApplicationPath("/")
public class JobStoreApplication extends Application {
    private static final Set<Class<?>> classes = new HashSet<>();

    static {
        classes.add(JobsBean.class);
        classes.add(JobsExportsBean.class);
        classes.add(NotificationsBean.class);
        classes.add(StatusBean.class);
        classes.add(JobSchedulerRestBean.class);
        classes.add(RerunsBean.class);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }
}
