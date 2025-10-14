package dk.dbc.dataio.harvester.rr_dm3.rest;

import dk.dbc.dataio.harvester.task.rest.HarvesterApplicationCore;
import jakarta.ws.rs.ApplicationPath;

/**
 * This class defines the other classes that make up this JAX-RS application by
 * having the getClasses method return a specific set of resources.
 */
@ApplicationPath("/")
public class HarvesterApplication extends HarvesterApplicationCore {
    static {
        classes.add(StatusBean.class);
    }
}
