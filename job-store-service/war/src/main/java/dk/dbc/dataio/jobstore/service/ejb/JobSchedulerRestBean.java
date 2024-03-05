package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.jobstore.distributed.JobSchedulerSinkStatus;
import dk.dbc.dataio.jobstore.distributed.QueueSubmitMode;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingService;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
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
    DependencyTrackingService dependencyTrackingService;

    @Inject
    public JobSchedulerRestBean(DependencyTrackingService dependencyTrackingService) {
        this.dependencyTrackingService = dependencyTrackingService;
    }

    @POST
    @Path(JobStoreServiceConstants.SCHEDULER_SINK_FORCE_BULK_MODE)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Stopwatch
    public Response forceSinkIntoBulkMode(String stuff, @PathParam(JobStoreServiceConstants.SINK_ID_VARIABLE) int sinkId) {
        JobSchedulerSinkStatus sinkStatus = dependencyTrackingService.getSinkStatus(sinkId);
        sinkStatus.getProcessingStatus().setMode(QueueSubmitMode.BULK);
        sinkStatus.getDeliveringStatus().setMode(QueueSubmitMode.BULK);
        return Response.ok().build();
    }

    @POST
    @Path(JobStoreServiceConstants.SCHEDULER_SINK_FORCE_TRANSITION_MODE)
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Stopwatch
    public Response forceSinkIntoTransitionToDirectMode(String stuff, @PathParam(JobStoreServiceConstants.SINK_ID_VARIABLE) int sinkId) {
        JobSchedulerSinkStatus sinkStatus = dependencyTrackingService.getSinkStatus(sinkId);
        sinkStatus.getProcessingStatus().setMode(QueueSubmitMode.TRANSITION_TO_DIRECT);
        sinkStatus.getDeliveringStatus().setMode(QueueSubmitMode.TRANSITION_TO_DIRECT);
        return Response.ok().build();
    }
}
