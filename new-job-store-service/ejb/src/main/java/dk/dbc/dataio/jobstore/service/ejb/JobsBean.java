package dk.dbc.dataio.jobstore.service.ejb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource
 * exposed by the /{@value dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants#JOB_COLLECTION} entry point
 */
@Stateless
@Path("/")
public class JobsBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobsBean.class);

    @GET
    public Response iOnlyExistForSanityTest() {
        LOGGER.debug("some debug information");
        return Response.ok().build();
    }
}
