package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource
 * exposed by the /{@value dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants#JOB_COLLECTION} entry point
 */
@Stateless
@Path("/")
public class JobsBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobsBean.class);
    private static final JSONBContext jsonbContext = new JSONBContext();
    private static final String DUMMY_JOB_ID = "42";

    @GET
    public Response iOnlyExistForSanityTest() {
        LOGGER.debug("some debug information");
        return Response.ok().build();
    }

    @POST
    @Path(JobStoreServiceConstants.JOB_COLLECTION)
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response addJob(@Context UriInfo uriInfo, String jobInputStreamData) throws JsonException, JSONBException {
        LOGGER.trace("JobInputStream: {}", jobInputStreamData);
        JobInputStream jobInputStream = helloWorldDummyMethod(jobInputStreamData);

        return Response.created(getUri(uriInfo, DUMMY_JOB_ID))
                .entity(jsonbContext.marshall(jobInputStream))
                .build();
    }

    private JobInputStream helloWorldDummyMethod(String jobInputStreamJson) throws JSONBException {
        // This method is clearly doing something amazing!
        return jsonbContext.unmarshall(jobInputStreamJson, JobInputStream.class);
    }

    private URI getUri(UriInfo uriInfo, String jobId) {
        final UriBuilder absolutePathBuilder = uriInfo.getAbsolutePathBuilder();
        return absolutePathBuilder.path(jobId).build();
    }
}
