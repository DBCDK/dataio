package dk.dbc.dataio.harvester.task.rest;

import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

public abstract class HarvesterApplicationCore extends Application {
    public static final Set<Class<?>> classes = new HashSet<>();
    static {
        classes.add(HarvestTasksBean.class);
    }

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }
}
