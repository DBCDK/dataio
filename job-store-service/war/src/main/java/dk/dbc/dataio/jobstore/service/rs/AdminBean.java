package dk.dbc.dataio.jobstore.service.rs;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean;
import dk.dbc.dataio.jobstore.service.ejb.PgJobStoreRepository;
import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity;
import dk.dbc.jms.artemis.AdminClient;
import dk.dbc.jms.artemis.AdminClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkSchedulingStatus.QUEUED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkSchedulingStatus.QUEUED_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkSchedulingStatus.READY_FOR_PROCESSING;

@Stateless
@LocalBean
@Path("/")
public class AdminBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBean.class);
    @EJB
    JobSchedulerBean jobSchedulerBean;
    @EJB
    PgJobStoreRepository jobStoreRepository;

    @Inject
    @JobstoreDB
    EntityManager entityManager;

    @Inject
    FlowStoreServiceConnectorBean flowStoreService;

    AdminClient adminClient = AdminClientFactory.getAdminClient();

    @GET
    @Path(JobStoreServiceConstants.CLEAR_CACHE)
    @Produces({MediaType.TEXT_PLAIN})
    public Response clearJobstoreCache() {
        Cache cache = entityManager.getEntityManagerFactory().getCache();
        cache.evictAll();
        LOGGER.info("Evicted jpa cache");
        return Response.ok("ok").build();
    }

    public Response abortJob(int jobId) throws FlowStoreServiceConnectorException {
        LOGGER.warn("Aborting job {}", jobId);

        removeFromDependencyTracking(jobId);
        removeFromAllQueues(jobId);
        return Response.ok("ok").build();
    }

    private void removeFromDependencyTracking(int jobId) {

    }

    private void removeFromAllQueues(int jobId) throws FlowStoreServiceConnectorException {
        Stream<String> processQueues = Arrays.stream(JobSpecification.Type.values()).map(t -> t.processorQueue);
        Stream<String> sinkQueues = flowStoreService.getConnector().findAllSinks().stream().map(Sink::getContent).map(SinkContent::getQueue);
        List<String> queues = Stream.concat(processQueues, sinkQueues).distinct().collect(Collectors.toList());
        LOGGER.info("Removing job {} from queues: {}", jobId, queues);
        queues.forEach(q -> removeFromQueue(q, jobId));
    }

    private void removeFromQueue(String fqn, int jobId) {
        String[] sa = fqn.split("::", 2);
        String address = sa[0];
        String queue = sa[sa.length - 1];
        adminClient.removeMessages(queue, address, "jobId = '" + jobId + "'");
    }

    @POST
    @Path(JobStoreServiceConstants.FORCE_DEPENDENCY_TRACKING_RETRANSMIT)
    public Response reTransmit(@QueryParam("jobIds") @DefaultValue("") String jobIds) {
        List<Integer> ids = Arrays.stream(jobIds.split(" *, *")).map(Integer::valueOf).collect(Collectors.toList());
        return reTransmitAllJobs(ids);
    }

    private Response reTransmitAllJobs(List<Integer> jobIds) {
        int rowsUpdated = jobStoreRepository.resetStatus(jobIds, QUEUED_FOR_PROCESSING, READY_FOR_PROCESSING);
        LOGGER.info("Reset dependency tracking states. Sets status = 1 for status = 2 for {} entities", rowsUpdated);
        rowsUpdated = jobStoreRepository.resetStatus(jobIds, QUEUED_FOR_DELIVERY, DependencyTrackingEntity.ChunkSchedulingStatus.READY_FOR_DELIVERY);
        LOGGER.info("Reset dependency tracking states. Sets status = 4 for status = 5 for {} entities", rowsUpdated);
        jobSchedulerBean.loadSinkStatusOnBootstrap();
        return Response.ok().build();
    }
}
