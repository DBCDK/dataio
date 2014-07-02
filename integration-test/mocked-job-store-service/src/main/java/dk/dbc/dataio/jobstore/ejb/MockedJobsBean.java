package dk.dbc.dataio.jobstore.ejb;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.json.mixins.MixIns;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.model.FlowBuilder;
import dk.dbc.dataio.jobstore.types.Job;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Stateless
@Path("/")
public class MockedJobsBean extends JobsBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(MockedJobsBean.class);

    @POST
    @Path(JobStoreServiceConstants.JOB_COLLECTION)
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response createJob(@Context UriInfo uriInfo, String jobSpecData)
            throws NullPointerException, IllegalArgumentException, EJBException, JsonException {
        LOGGER.info("Mocked job creation");
        final JobSpecification jobSpec = JsonUtil.fromJson(jobSpecData, JobSpecification.class, MixIns.getMixIns());
        try {
            final Job job = jobStoreBean.createAndScheduleJob(jobSpec, null, new FlowBuilder().build(), null, null);
            final String jobInfoJson = JsonUtil.toJson(job.getJobInfo());
            return Response.created(uriInfo.getAbsolutePathBuilder().path(String.valueOf(job.getId())).build())
                    .entity(jobInfoJson).build();
        } catch (JobStoreException | JsonException e) {
            throw new EJBException(e);
        }
    }
}
