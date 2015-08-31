package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.jobstore.service.sequenceanalyser.ChunkIdentifier;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jobstore.types.ResourceBundle;
import dk.dbc.dataio.jobstore.types.criteria.ChunkListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jobstore.types.criteria.ListOrderBy;
import dk.dbc.dataio.sequenceanalyser.CollisionDetectionElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dk.dbc.dataio.jobstore.service.ejb.JobSchedulerBean.NOT_PUBLISH_WORKLOAD;

@Singleton
@Startup
@DependsOn("StartupDBMigrator")
public class BootstrapBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapBean.class);
    private static final int CACHE_MAX_ENTRIES = 100;

    private final LinkedHashMap<Long, Sink> cache =
            new LinkedHashMap(CACHE_MAX_ENTRIES) {
                @Override
                public boolean removeEldestEntry(Map.Entry eldest) {
                    return size() > CACHE_MAX_ENTRIES;
                }};

    @EJB
    PgJobStore jobStore;

    @EJB
    JobSchedulerBean jobSchedulerBean;

    @EJB
    private JmsEmptyQueuesBean jmsEmptyQueuesBean;

    @PostConstruct
    @Stopwatch
    public void initialize() {
        try {
            //this.jmsEmptyQueuesBean.emptyQueues();
            restoreSystemState();
            jobSchedulerBean.jumpStart();
        } catch (JobStoreException e) {
            throw new EJBException(e);
        }
    }

    /**
     * Locates any chunk, that has not yet finished, and passes found chunk collision detection element
     * and sink on to the sequence analyser
     *
     * @throws JobStoreException on failure to retrieve job or if unable to setup monitoring
     */
    private void restoreSystemState() throws JobStoreException{
        LOGGER.info("Restoring job-store state");

        final int limit = 1000;
        int offset = 0;
        while (true) {
            final ChunkListCriteria chunkListCriteria = new ChunkListCriteria()
                    .where(new ListFilter<>(ChunkListCriteria.Field.TIME_OF_COMPLETION, ListFilter.Op.IS_NULL))
                    .orderBy(new ListOrderBy<>(ChunkListCriteria.Field.TIME_OF_CREATION, ListOrderBy.Sort.ASC))
                    .limit(limit)
                    .offset(offset);

            final List<CollisionDetectionElement> collisionDetectionElements =
                    jobStore.listChunksCollisionDetectionElements(chunkListCriteria);
            for (CollisionDetectionElement collisionDetectionElement : collisionDetectionElements) {
                long jobId = ((ChunkIdentifier) collisionDetectionElement.getIdentifier()).getJobId();

                if (!cache.containsKey(jobId)) {
                    ResourceBundle resourceBundle = jobStore.getResourceBundle((int) jobId);
                    cache.put(jobId, resourceBundle.getSink());
                }
                jobSchedulerBean.scheduleChunk(collisionDetectionElement, cache.get(jobId), NOT_PUBLISH_WORKLOAD);
            }

            if (collisionDetectionElements.size() != limit) {
                break;
            }
            offset += limit;
        }
    }
}
