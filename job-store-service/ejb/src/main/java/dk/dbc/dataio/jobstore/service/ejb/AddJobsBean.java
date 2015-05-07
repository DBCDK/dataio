package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.jobstore.types.InvalidInputException;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@Stateless
@Path("/")
public class AddJobsBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobsBean.class);

    JSONBContext jsonbContext = new JSONBContext();

    @EJB
    BootstrapBean bootstrapBean;

    @EJB
    JobStoreBean jobStoreBean;

    @PostConstruct
    public void initialize() {
        bootstrapBean.waitForSystemInitialization();
    }

    /**
     * Adds new job based on POSTed job input stream, and persists it in the underlying data store
     *
     * @param uriInfo application and request URI information
     * @param jobInputStreamData job input stream data as json
     *
     * @return a HTTP 201 CREATED response with a Location header containing the URL value of the newly created resource,
     *         a HTTP 400 BAD_REQUEST response on invalid json content,
     *         a HTTP 400 BAD_REQUEST response on referenced entities not found,
     *
     * @throws dk.dbc.dataio.jsonb.JSONBException on marshalling failure
     * @throws dk.dbc.dataio.jobstore.types.JobStoreException on failure to add job
     */
    @POST
    @Path(JobStoreServiceConstants.JOB_COLLECTION)
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response addJob(@Context UriInfo uriInfo, String jobInputStreamData) throws JSONBException, JobStoreException {
        LOGGER.trace("JobInputStream: {}", jobInputStreamData);
        final JobInputStream jobInputStream;
        JobInfoSnapshot jobInfoSnapshot;

        try {
            jobInputStream = jsonbContext.unmarshall(jobInputStreamData, JobInputStream.class);
            jobInfoSnapshot = jobStoreBean.addAndScheduleJob(jobInputStream);
            return Response.created(getUri(uriInfo, Integer.toString(jobInfoSnapshot.getJobId())))
                    .entity(jsonbContext.marshall(jobInfoSnapshot))
                    .build();

        } catch (JSONBException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(jsonbContext.marshall(new JobError(JobError.Code.INVALID_JSON, e.getMessage(), ServiceUtil.stackTraceToString(e))))
                    .build();
        } catch(InvalidInputException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(jsonbContext.marshall(e.getJobError()))
                    .build();
        }
    }

    private URI getUri(UriInfo uriInfo, String jobId) {
        final UriBuilder absolutePathBuilder = uriInfo.getAbsolutePathBuilder();
        return absolutePathBuilder.path(jobId).build();
    }
}
