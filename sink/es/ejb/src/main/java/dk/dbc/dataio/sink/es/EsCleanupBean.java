package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.sink.es.ESTaskPackageUtil.TaskStatus;
import dk.dbc.dataio.sink.es.entity.EsInFlight;
import dk.dbc.dataio.sink.types.SinkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@LocalBean
@Singleton
public class EsCleanupBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(EsCleanupBean.class);

    @EJB
    EsConnectorBean esConnector;

    @EJB
    EsInFlightBean esInFlightAdmin;

    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    JSONBContext jsonbContext = new JSONBContext();

    @PostConstruct
    public void startup() {
        LOGGER.info("Startup of EsScheduledCleanupBean!");
        List<EsInFlight> esInFlightList = esInFlightAdmin.listEsInFlight();
        LOGGER.info("The following targetreferences are inFlight in the sink at startup: {}",
                Arrays.toString(createEsInFlightMap(esInFlightList).keySet().toArray()));
        // Integrity-test is deferred to the first run of cleanup().
    }

    /**
     * Cleanup of ES/Inflight.
     *
     * When this thread awakens, it will find the current in-flight
     * target references, lookup those target references in the ES-base,
     * and if any of the corresponding task packages are completed or aborted
     * they will be removed from the ES and the in-flight databases.
     *
     * This method runs in its own transactional scope to avoid
     * tearing down any controlling timers
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void cleanup() {
        final StopWatch stopWatch = new StopWatch();
        try {
            LOGGER.info("Cleaning up ES-base");
            Map<Integer, EsInFlight> esInFlightMap = createEsInFlightMap(esInFlightAdmin.listEsInFlight());
            if (esInFlightMap.isEmpty()) {
                LOGGER.info("No records in ES InFlight.");
                return;
            }
            Map<Integer, TaskStatus> taskStatusMap = getTaskStatusForESTaskpackages(esInFlightMap);

            final List<ExternalChunk> lostChunks = findLostChunks(esInFlightMap, taskStatusMap);
            if(!lostChunks.isEmpty()) {
                addChunks(lostChunks);
            }

            final List<ExternalChunk> deliveredChunks = findDeliveredChunks(esInFlightMap, taskStatusMap);
            if(!deliveredChunks.isEmpty()) {
                addChunks(deliveredChunks);
            }

        } catch (SinkException ex) {
            LOGGER.error("A SinkException was thrown during cleanup of ES/inFlight", ex);
        } finally {
            LOGGER.info("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    private void addChunks(List<ExternalChunk> externalChunks) {
        JobStoreServiceConnector jobStoreServiceConnector = jobStoreServiceConnectorBean.getConnector();
        for(ExternalChunk chunk : externalChunks) {
            try {
                jobStoreServiceConnector.addChunkIgnoreDuplicates(chunk, chunk.getJobId(), chunk.getChunkId());
            } catch (JobStoreServiceConnectorException e) {
                if (e instanceof JobStoreServiceConnectorUnexpectedStatusCodeException) {
                    final JobError jobError = ((JobStoreServiceConnectorUnexpectedStatusCodeException) e).getJobError();
                    if (jobError != null) {
                        LOGGER.error("job-store returned error: {}", jobError.getDescription());
                    }
                }
                throw new EJBException(e);
            }
        }
    }

    private void removeEsInFlights(List<EsInFlight> esInFlightList) {
        for (EsInFlight esInFlight : esInFlightList) {
            esInFlightAdmin.removeEsInFlight(esInFlight);
        }
    }

    private List<Integer> findTargetReferencesForCompletedTaskpackagesFromTaskStatus(Map<Integer, TaskStatus> taskStatusMap) {
        final List<Integer> finishedTaskPackages = new ArrayList<>();
        for (Map.Entry<Integer, TaskStatus> entry : taskStatusMap.entrySet()) {
            final TaskStatus ts = entry.getValue();
            if (ts.getTaskStatus() == TaskStatus.Code.COMPLETE || ts.getTaskStatus() == TaskStatus.Code.ABORTED) {
                finishedTaskPackages.add(entry.getKey());
            }
        }
        return finishedTaskPackages;
    }

    private Map<Integer, TaskStatus> getTaskStatusForESTaskpackages(Map<Integer, EsInFlight> esInFlightMap) throws SinkException {
        return esConnector.getCompletionStatusForESTaskpackages(new ArrayList<>(esInFlightMap.keySet()));
    }

    private List<ExternalChunk> findLostChunks(Map<Integer, EsInFlight> esInFlightMap, Map<Integer, TaskStatus> taskStatusMap) throws SinkException {
        final StopWatch stopWatch = new StopWatch();
        try {
            List<ExternalChunk> lostExternalChunks = new ArrayList<>();
            List<EsInFlight> lostEsInFlight = new ArrayList<>();
            LOGGER.info("Number of [taskstatus/esInFlight] : [{}/{}]", taskStatusMap.size(), esInFlightMap.size());

            for (Map.Entry<Integer, EsInFlight> esInFlightEntry : esInFlightMap.entrySet()) {
                if (!taskStatusMap.containsKey(esInFlightEntry.getKey())) {
                    // The entry did not have a target reference -> create lost chunk and add it to the list
                    lostExternalChunks.add(createLostChunk(esInFlightEntry.getValue()));
                    lostEsInFlight.add(esInFlightEntry.getValue());
                }
            }
            if (!lostEsInFlight.isEmpty()) {
                LOGGER.info("Number of lost taskpackages in ES : {}.", esInFlightMap.size());
                removeEsInFlights(lostEsInFlight);
            }
            return lostExternalChunks;
        } finally {
            LOGGER.info("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    ExternalChunk createLostChunk(EsInFlight esInFlight) throws SinkException {
        LOGGER.info("Finishing lost chunk for tp: {}", esInFlight.getTargetReference());
        final ExternalChunk externalChunk;
        try {
            externalChunk = jsonbContext.unmarshall(esInFlight.getIncompleteDeliveredChunk(), ExternalChunk.class);
        } catch (JSONBException e) {
            throw new SinkException(String.format("Unable to marshall lost chunk for tp: %d", esInFlight.getTargetReference()), e);
        }

        // Create a new external chunk representing the lost target reference
        final ExternalChunk lostExternalChunk = new ExternalChunk(externalChunk.getJobId(), externalChunk.getChunkId(), externalChunk.getType());
        lostExternalChunk.setEncoding(externalChunk.getEncoding());

        for (ChunkItem chunkItem : externalChunk) {
            lostExternalChunk.insertItem(createLostChunkItem(chunkItem));
        }
        return lostExternalChunk;
    }

    private ChunkItem createLostChunkItem(ChunkItem chunkItem) {
        final ChunkItem lostChunkItem;
        if(chunkItem.getStatus() == ChunkItem.Status.SUCCESS) {
            final String data = "Item status set to failed due to taskpackage lost in ES";
            lostChunkItem = new ChunkItem(chunkItem.getId(), StringUtil.asBytes(data), ChunkItem.Status.FAILURE);
        } else {
            lostChunkItem = chunkItem;
        }
        return lostChunkItem;
    }

    private List<ExternalChunk> findDeliveredChunks(Map<Integer, EsInFlight> esInFlightMap, Map<Integer, TaskStatus> taskStatusMap) {
        final StopWatch stopWatch = new StopWatch();
        List<ExternalChunk> deliveredChunks = new ArrayList<>();
        try {
            final List<Integer> finishedTargetReferences = findTargetReferencesForCompletedTaskpackagesFromTaskStatus(taskStatusMap);
            if (finishedTargetReferences.isEmpty()) {
                LOGGER.info("No finished taskpackages in ES.");
            } else {
                List<EsInFlight> finishedEsInFlight = getEsInFlightsFromTargetReferences(esInFlightMap, finishedTargetReferences);
                deliveredChunks = createDeliveredChunks(finishedEsInFlight);
                esConnector.deleteESTaskpackages(finishedTargetReferences);
                removeEsInFlights(finishedEsInFlight);
                return deliveredChunks;
            }
        } catch (SinkException ex) {
            LOGGER.error("A SinkException was thrown during cleanup of ES/inFlight", ex);
        } finally {
            LOGGER.info("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
        return deliveredChunks;

    }

    private List<ExternalChunk> createDeliveredChunks(List<EsInFlight> finishedEsInFlight) throws SinkException {
        List<ExternalChunk> deliveredChunks = new ArrayList<>(finishedEsInFlight.size());
        for (EsInFlight esInFlight : finishedEsInFlight) {
            deliveredChunks.add(createDeliveredChunk(esInFlight));
        }
        return deliveredChunks;
    }

    ExternalChunk createDeliveredChunk(EsInFlight esInFlight) throws SinkException {
        LOGGER.info("Finishing delivered chunk for tp: {}", esInFlight.getTargetReference());
        final ExternalChunk incompleteDeliveredChunk;
        try {
            incompleteDeliveredChunk = jsonbContext.unmarshall(esInFlight.getIncompleteDeliveredChunk(), ExternalChunk.class);
        } catch (JSONBException e) {
            throw new SinkException(String.format("Unable to marshall delivered chunk for tp: %d", esInFlight.getTargetReference()), e);
        }

        return esConnector.getChunkForTaskPackage(esInFlight.getTargetReference(), incompleteDeliveredChunk);
    }
    
    private Map<Integer, EsInFlight> createEsInFlightMap(List<EsInFlight> esInFlightList) {
        Map<Integer, EsInFlight> esInFlightMap = new HashMap<>();
        for (EsInFlight esInFlight : esInFlightList) {
            esInFlightMap.put(esInFlight.getTargetReference(), esInFlight);
        }
        return esInFlightMap;
    }

    private List<EsInFlight> getEsInFlightsFromTargetReferences(Map<Integer, EsInFlight> esInFlightMap, List<Integer> finishedTargetReferences) {
        List<EsInFlight> finishedEsInFlight = new ArrayList<>();
        for (Integer targetReference : finishedTargetReferences) {
            finishedEsInFlight.add(esInFlightMap.get(targetReference));
        }
        return finishedEsInFlight;
    }
}
