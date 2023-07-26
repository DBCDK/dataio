package dk.dbc.dataio.jobstore.service.rs;

import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.jobstore.service.cdi.JobstoreDB;
import dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean;
import dk.dbc.dataio.jobstore.service.ejb.PgJobStoreRepository;
import dk.dbc.jms.artemis.AdminClient;
import dk.dbc.jms.artemis.AdminClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkSchedulingStatus.QUEUED_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkSchedulingStatus.QUEUED_FOR_PROCESSING;
import static dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkSchedulingStatus.READY_FOR_DELIVERY;
import static dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.ChunkSchedulingStatus.READY_FOR_PROCESSING;

@Stateless
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

    @Schedule(second = "0", minute = "5", hour = "*", persistent = false)
    public void cleanStaleJMSConnections() {
        LOGGER.info("Cleaning stale artemis connections");
        Instant i = Instant.now().minus(Duration.ofMinutes(15));
        adminClient.closeConsumerConnections(c -> i.isAfter(c.getLastAcknowledgedTime()));
    }

    @GET
    @Path(JobStoreServiceConstants.CLEAR_CACHE)
    @Produces({MediaType.TEXT_PLAIN})
    public Response clearJobstoreCache() {
        Cache cache = entityManager.getEntityManagerFactory().getCache();
        cache.evictAll();
        LOGGER.info("Evicted jpa cache");
        return Response.ok("ok").build();
    }

    @POST
    @Path(JobStoreServiceConstants.FORCE_DEPENDENCY_TRACKING_RETRANSMIT)
    public Response reTransmit(@PathParam("jobIds") @DefaultValue("") String jobIds) {
        List<Integer> ids = Arrays.stream(jobIds.split(" *, *")).map(Integer::valueOf).collect(Collectors.toList());
        return reTransmitAllJobs(ids);
    }

    private Response reTransmitAllJobs(List<Integer> jobIds) {
        int rowsUpdated = jobStoreRepository.resetStatus(jobIds, QUEUED_FOR_PROCESSING, READY_FOR_PROCESSING);
        LOGGER.info("Reset dependency tracking states. Sets status = 1 for status = 2 for {} entities", rowsUpdated);
        rowsUpdated = jobStoreRepository.resetStatus(jobIds, QUEUED_FOR_DELIVERY, READY_FOR_DELIVERY);
        LOGGER.info("Reset dependency tracking states. Sets status = 4 for status = 5 for {} entities", rowsUpdated);
        if(jobIds.isEmpty()) jobSchedulerBean.loadSinkStatusOnBootstrap(null);
        else jobIds.forEach(id -> jobSchedulerBean.loadSinkStatusOnBootstrap(id));
        return Response.ok().build();
    }
}
