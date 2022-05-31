package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;

import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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

    ;


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

    ;

}
