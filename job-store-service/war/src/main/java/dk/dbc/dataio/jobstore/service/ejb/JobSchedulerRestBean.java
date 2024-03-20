package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Created by ja7 on 19-07-16.
 */
@Stateless
@Path("/")
public class JobSchedulerRestBean {

    @POST
    @Path(JobStoreServiceConstants.SCHEDULER_SINK_FORCE_BULK_MODE)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Stopwatch
    public Response forceSinkIntoBulkMode(String stuff, @PathParam(JobStoreServiceConstants.SINK_ID_VARIABLE) int sinkId) {
        JobSchedulerSinkStatus sinkStatus = JobSchedulerBean.getSinkStatus(sinkId);
        sinkStatus.processingStatus.setMode(JobSchedulerBean.QueueSubmitMode.BULK);
        sinkStatus.deliveringStatus.setMode(JobSchedulerBean.QueueSubmitMode.BULK);
        return Response.ok().build();
    }

    @POST
    @Path(JobStoreServiceConstants.SCHEDULER_SINK_FORCE_TRANSITION_MODE)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Stopwatch
    public Response forceSinkIntoTransitionToDirectMode(String stuff, @PathParam(JobStoreServiceConstants.SINK_ID_VARIABLE) int sinkId) {
        JobSchedulerSinkStatus sinkStatus = JobSchedulerBean.getSinkStatus(sinkId);
        sinkStatus.processingStatus.setMode(JobSchedulerBean.QueueSubmitMode.TRANSITION_TO_DIRECT);
        sinkStatus.deliveringStatus.setMode(JobSchedulerBean.QueueSubmitMode.TRANSITION_TO_DIRECT);
        return Response.ok().build();
    }
}
