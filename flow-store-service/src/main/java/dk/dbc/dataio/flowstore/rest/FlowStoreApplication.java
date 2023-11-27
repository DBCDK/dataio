package dk.dbc.dataio.flowstore.rest;

import dk.dbc.dataio.flowstore.ejb.FlowBindersBean;
import dk.dbc.dataio.flowstore.ejb.FlowComponentsBean;
import dk.dbc.dataio.flowstore.ejb.FlowsBean;
import dk.dbc.dataio.flowstore.ejb.GatekeeperDestinationsBean;
import dk.dbc.dataio.flowstore.ejb.HarvestersBean;
import dk.dbc.dataio.flowstore.ejb.ParametersSuggester;
import dk.dbc.dataio.flowstore.ejb.SinksBean;
import dk.dbc.dataio.flowstore.ejb.SubmittersBean;
import dk.dbc.dataio.flowstore.ejb.SubversionFetcher;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * This class defines the other classes that make up this JAX-RS application
 * by having the getClasses method return a specific set of resources.
 */
@ApplicationPath("/")
public class FlowStoreApplication extends Application {
    private static final Logger log = LoggerFactory.getLogger(FlowStoreApplication.class);

    @Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> classes = new HashSet<>();
        classes.add(FlowsBean.class);
        classes.add(FlowBindersBean.class);
        classes.add(FlowComponentsBean.class);
        classes.add(SinksBean.class);
        classes.add(StatusBean.class);
        classes.add(SubmittersBean.class);
        classes.add(HarvestersBean.class);
        classes.add(GatekeeperDestinationsBean.class);
        classes.add(ClassNotFoundExceptionMapper.class);
        classes.add(JsonExceptionMapper.class);
        classes.add(PersistenceExceptionMapper.class);
        classes.add(ReferencedEntityNotFoundExceptionMapper.class);
        classes.add(ParametersSuggester.class);
        classes.add(SubversionFetcher.class);
        for (Class<?> clazz : classes) {
            log.info("Registered {} resource", clazz.getName());
        }
        return classes;
    }
}
