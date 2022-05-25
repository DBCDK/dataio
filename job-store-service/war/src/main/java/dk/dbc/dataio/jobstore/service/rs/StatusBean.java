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
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
import dk.dbc.dataio.jobstore.types.SinkStatusSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
    @JobstoreDB
    EntityManager entityManager;

    @GET
    @Path(JobStoreServiceConstants.SINKS_STATUS)
    @Produces({ MediaType.APPLICATION_JSON })
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
    @Produces({ MediaType.APPLICATION_JSON })
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
        final Query query = entityManager.createNamedQuery(DependencyTrackingEntity.JOB_COUNT_CHUNK_COUNT_QUERY);
        query.setParameter(1, sink.getId());
        return (Object[]) query.getSingleResult();
    }
}
