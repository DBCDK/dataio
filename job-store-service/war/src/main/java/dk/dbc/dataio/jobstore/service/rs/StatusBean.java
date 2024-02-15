package dk.dbc.dataio.jobstore.service.rs;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.service.ServiceStatus;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.dependencytracking.DependencyTrackingService;
import dk.dbc.dataio.jobstore.types.SinkStatusSnapshot;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Stateless
@LocalBean
@Path("/")
public class StatusBean implements ServiceStatus {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatusBean.class);
    JSONBContext jsonbContext = new JSONBContext();

    @EJB
    FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;
    @Inject
    DependencyTrackingService dependencyTrackingService;

    @Inject
    @JobstoreDB
    EntityManager entityManager;

    @GET
    @Path(JobStoreServiceConstants.SINKS_STATUS)
    @Produces({MediaType.APPLICATION_JSON})
    @Stopwatch
    public Response getSinkStatusList() throws JSONBException {
        LOGGER.trace("getSinkStatusList called");
        try {
            final List<Sink> sinks = flowStoreServiceConnectorBean.getConnector().findAllSinks();
            final List<SinkStatusSnapshot> sinkStatusSnapshots = new ArrayList<>();
            for (Sink sink : sinks) {
                sinkStatusSnapshots.add(toSinkStatusSnapshot(sink, executeQuery(sink)));
            }
            return Response.status(Response.Status.OK).entity(jsonbContext.marshall(sinkStatusSnapshots)).build();
        } catch (FlowStoreServiceConnectorException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path(JobStoreServiceConstants.SINK_STATUS)
    @Produces({MediaType.APPLICATION_JSON})
    @Stopwatch
    public Response getSinkStatus(@PathParam(JobStoreServiceConstants.SINK_ID_VARIABLE) long sinkId) throws JSONBException {
        LOGGER.trace("getSinkStatus called with id {}", sinkId);
        try {
            final Sink sink = flowStoreServiceConnectorBean.getConnector().getSink(sinkId);
            final SinkStatusSnapshot sinkStatusSnapshot = toSinkStatusSnapshot(sink, executeQuery(sink));
            return Response.status(Response.Status.OK).entity(jsonbContext.marshall(sinkStatusSnapshot)).build();
        } catch (FlowStoreServiceConnectorException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    /*
     * Private methods
     */

    private SinkStatusSnapshot toSinkStatusSnapshot(Sink sink, Object[] resultList) {
        return new SinkStatusSnapshot()
                .withSinkId(sink.getId())
                .withSinkType(sink.getContent().getSinkType())
                .withName(sink.getContent().getName())
                .withNumberOfJobs(((Long) resultList[0]).intValue())
                .withNumberOfChunks(((Long) resultList[1]).intValue());
    }

    private Object[] executeQuery(Sink sink) {
        return dependencyTrackingService.jobCount(sink.getId());
    }
}
