package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.jobstore.types.InvalidInputException;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.JobStoreException;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

/**
 * This Enterprise Java Bean (EJB) class acts as a JAX-RS root resource
 * exposed by the /{@value dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants#RERUNS} entry point
 */
@Path("/")
public class RerunsBean {
    @EJB JobRerunnerBean jobRerunnerBean;

    final JSONBContext jsonbContext = new JSONBContext();

    @POST
    @Path(JobStoreServiceConstants.RERUNS)
    @Consumes({MediaType.TEXT_PLAIN})
    @Produces({MediaType.APPLICATION_JSON})
    @Stopwatch
    public Response createJobRerun(Integer jobId, @DefaultValue("false") @QueryParam("failedItemsOnly") boolean failedItemsOnly)
            throws JobStoreException, JSONBException {
        try {
            if (failedItemsOnly) {
                jobRerunnerBean.requestJobFailedItemsRerun(jobId);
            } else {
                jobRerunnerBean.requestJobRerun(jobId);
            }
        } catch (InvalidInputException e) {
            return Response.status(Response.Status.NOT_ACCEPTABLE)
                    .entity(getErrorEntity(e).orElse(""))
                    .build();
        }
        return Response.status(Response.Status.CREATED).entity("").build();
    }

    private Optional<String> getErrorEntity(InvalidInputException e) throws JSONBException {
        final JobError jobError = e.getJobError();
        if (jobError != null) {
            return Optional.of(jsonbContext.marshall(jobError));
        }
        return Optional.empty();
    }
}
