package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.sink.es.ESTaskPackageUtil.TaskStatus;
import dk.dbc.dataio.sink.es.entity.EsInFlight;
import dk.dbc.dataio.sink.types.SinkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@LocalBean
@Singleton
@DependsOn("EsThrottlerBean")
public class EsCleanupBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(EsCleanupBean.class);

    @EJB
    EsConnectorBean esConnector;

    @EJB
    EsInFlightBean esInFlightAdmin;

    @EJB
    EsThrottlerBean esThrottler;

    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    JSONBContext jsonbContext = new JSONBContext();

    @PostConstruct
    public void startup() {
        LOGGER.info("Startup of EsScheduledCleanupBean!");
        List<EsInFlight> esInFlightList = esInFlightAdmin.listEsInFlight();
        LOGGER.info("The following targetreferences are inFlight in the sink at startup: {}",
                Arrays.toString(createEsInFlightMap(esInFlightList).keySet().toArray()));
        int slotsInFlight = sumRecordSlotsInEsInFlightList(esInFlightList);
        LOGGER.info("Sum of recordSlots for inFlight Chunks: [{}]", slotsInFlight);
        esThrottler.acquireRecordSlots(slotsInFlight);
        // Integrity-test is deferred to the first run of cleanup().
    }

    private int sumRecordSlotsInEsInFlightList(List<EsInFlight> esInFlightList) {
        int res = 0;
        for (EsInFlight esInFlight : esInFlightList) {
            res += esInFlight.getRecordSlots();
        }
        return res;
    }

    /**
     * Cleanup of ES/Inflight.
     *
     * When this thread awakens, it will find the current inflight
     * targetreferences, find those targetreferences in the ES-base, and if any
     * of the corresponding taskpackages are completed or aborted they will be
     * removed. Both from the ES-base and the InFlight-base.
     *
     * After that, the semaphore will be decremented with the amount of slots
     * previously held by the completed/aborted taskpackages.
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

            final List<ExternalChunk> lostChunks = findLostChunks(esInFlightMap);
            if(!lostChunks.isEmpty()) {
                addChunks(lostChunks);
            }

            final List<ExternalChunk> deliveredChunks = findDeliveredChunks(esInFlightMap);
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

    private List<Integer> findTargetReferencesForCompletedTaskpackagesFromTaskStatus(List<TaskStatus> taskStatus) throws SinkException {
        List<TaskStatus> finishedTaskpackages = filterFinsihedTaskpackages(taskStatus);
        return filterTargetReferencesFromTaskStatusList(finishedTaskpackages);
    }

    private List<TaskStatus> getTaskStatusForESTaskpackages(Map<Integer, EsInFlight> esInFlightMap) throws SinkException {
        List<Integer> targetReferences = new ArrayList<>(esInFlightMap.keySet());
        return esConnector.getCompletionStatusForESTaskpackages(targetReferences);
    }

    private List<ExternalChunk> findLostChunks(Map<Integer, EsInFlight> esInFlightMap) throws SinkException {
        final StopWatch stopWatch = new StopWatch();
        try {
            List<ExternalChunk> lostExternalChunks = new ArrayList<>();
            List<EsInFlight> lostEsInFlight = new ArrayList<>();
            List<TaskStatus> taskStatusList = getTaskStatusForESTaskpackages(esInFlightMap);
            LOGGER.info("Number of [taskstatus/esInFlight] : [{}/{}]", taskStatusList.size(), esInFlightMap.size());

            for (Map.Entry<Integer, EsInFlight> esInFlightEntry : esInFlightMap.entrySet()) {
                if (!hasTargetReference(taskStatusList, esInFlightEntry.getKey())) {
                    // The entry did not have a target reference -> create lost chunk and add it to the list
                    lostExternalChunks.add(createLostChunk(esInFlightEntry.getValue()));
                    lostEsInFlight.add(esInFlightEntry.getValue());
                }
            }
            if (!lostEsInFlight.isEmpty()) {
                LOGGER.info("Number of lost taskpackages in ES : {}.", esInFlightMap.size());
                removeEsInFlights(lostEsInFlight);
                int recordSlotsToRelease = sumRecordSlotsInEsInFlightList(lostEsInFlight);
                esThrottler.releaseRecordSlots(recordSlotsToRelease);
            }
            return lostExternalChunks;
        } finally {
            LOGGER.info("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    private boolean hasTargetReference(List<TaskStatus>taskStatusList, Integer esInFlightReference) {
        for(TaskStatus taskStatus : taskStatusList) {
            if (esInFlightReference == taskStatus.getTargetReference()) {
                return true;
            }
        }
        return false;
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
        Iterator<ChunkItem> it = externalChunk.iterator();

        while (it.hasNext()) {
            ChunkItem chunkItem = it.next();
            lostExternalChunk.insertItem(buildChunkItem(chunkItem));
        }
        return lostExternalChunk;
    }

    private ChunkItem buildChunkItem(ChunkItem chunkItem) {
      final ChunkItem lostChunkItem;
            if(chunkItem.getStatus() == ChunkItem.Status.SUCCESS) {
                final String data = "Item status set to failed due to taskpackage lost in ES";
                lostChunkItem = new ChunkItem(chunkItem.getId(), data, ChunkItem.Status.FAILURE);
            } else {
                lostChunkItem = chunkItem;
        }
        return lostChunkItem;
    }

    private List<ExternalChunk> findDeliveredChunks(Map<Integer, EsInFlight> esInFlightMap) {
        final StopWatch stopWatch = new StopWatch();
        List<ExternalChunk> deliveredChunks = new ArrayList<>();
        try {
            List<TaskStatus> taskStatusList = getTaskStatusForESTaskpackages(esInFlightMap);
            List<Integer> finishedTargetReferences = findTargetReferencesForCompletedTaskpackagesFromTaskStatus(taskStatusList);
            if (finishedTargetReferences.isEmpty()) {
                LOGGER.info("No finished taskpackages in ES.");
            } else {
                List<EsInFlight> finishedEsInFlight = getEsInFlightsFromTargetReferences(esInFlightMap, finishedTargetReferences);
                int recordSlotsToRelease = sumRecordSlotsInEsInFlightList(finishedEsInFlight);
                deliveredChunks = createDeliveredChunks(finishedEsInFlight);
                esConnector.deleteESTaskpackages(finishedTargetReferences);
                removeEsInFlights(finishedEsInFlight);
                esThrottler.releaseRecordSlots(recordSlotsToRelease);
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

        // this list only contains the resulting items for the successfull items in the incompleteDeliveredChunk.
        final List<ChunkItem> items = esConnector.getResultingItemsFromSinkForTaskPackage(esInFlight.getTargetReference());
        
        // Ensure that all items from the taskpackage has a match in the incompleteDeliveredChunk,
        // i.e. the number of successfull items in incompleteDeliveredChunk must equals the number of
        // items from the taskpackage
        int successfulItemsFromIncompleteDeliveredChunk = getNumberOfSuccessfulItemsFromIncompleteDeliveredChunk(incompleteDeliveredChunk);
        throwIfMismatchBetweenNumberOfItems(items, successfulItemsFromIncompleteDeliveredChunk, esInFlight, incompleteDeliveredChunk);
        ExternalChunk deliveredChunk = weaveItemsFromTaskpackageAndIncompleteDeliveredChunk(incompleteDeliveredChunk, items, esInFlight);        

        return deliveredChunk;
    }
    
    private int getNumberOfSuccessfulItemsFromIncompleteDeliveredChunk(final ExternalChunk incompleteDeliveredChunk) {
        int successfulItemsFromIncompleteDeliveredChunk = 0;
        for(ChunkItem item : incompleteDeliveredChunk) {
            successfulItemsFromIncompleteDeliveredChunk += item.getStatus() == ChunkItem.Status.SUCCESS ? 1 : 0;
        }
        return successfulItemsFromIncompleteDeliveredChunk;
    }

    private void throwIfMismatchBetweenNumberOfItems(final List<ChunkItem> items, int successfulItemsFromIncompleteDeliveredChunk, EsInFlight esInFlight, final ExternalChunk incompleteDeliveredChunk) throws SinkException {
        if (items.size() != successfulItemsFromIncompleteDeliveredChunk) {
            throw new SinkException(String.format("Item discrepancy between tp<%d> and inflight chunk (%d/%d): %d items returned, %d items used",
                    esInFlight.getTargetReference(),
                    incompleteDeliveredChunk.getJobId(),
                    incompleteDeliveredChunk.getChunkId(),
                    items.size(),
                    incompleteDeliveredChunk.size()));
        }
    }

    private ExternalChunk weaveItemsFromTaskpackageAndIncompleteDeliveredChunk(final ExternalChunk incompleteDeliveredChunk, final List<ChunkItem> items, EsInFlight esInFlight) throws SinkException, IllegalArgumentException {
        // Create new deliveredChunk by weaving incompleteDeliveredChunk and ChunkItems from Taskpackage.
        ExternalChunk deliveredChunk = new ExternalChunk(incompleteDeliveredChunk.getJobId(), incompleteDeliveredChunk.getChunkId(), ExternalChunk.Type.DELIVERED);
        deliveredChunk.setEncoding(incompleteDeliveredChunk.getEncoding());
        int itemsCounter = 0;
        for(ChunkItem item : incompleteDeliveredChunk) {
            if (item.getStatus() == ChunkItem.Status.SUCCESS) {
                try {
                    int id = (int) item.getId();
                    deliveredChunk.insertItem(new ChunkItem(id, items.get(itemsCounter).getData(), items.get(itemsCounter).getStatus()));
                    itemsCounter++;
                } catch (IndexOutOfBoundsException e) {
                    String errMsg = String.format("Delivered Chunk item discrepancy for tp<%d>: Trying to get item [%d] from items from TP.", 
                            esInFlight.getTargetReference(),
                            itemsCounter);
                    throw new SinkException(errMsg, e);
                }
            } else {
                deliveredChunk.insertItem(item);
            }
        }
        return deliveredChunk;
    }


    private Map<Integer, EsInFlight> createEsInFlightMap(List<EsInFlight> esInFlightList) {
        Map<Integer, EsInFlight> esInFlightMap = new HashMap<>();
        for (EsInFlight esInFlight : esInFlightList) {
            esInFlightMap.put(esInFlight.getTargetReference(), esInFlight);
        }
        return esInFlightMap;
    }

    private List<TaskStatus> filterFinsihedTaskpackages(List<TaskStatus> taskStatus) {
        List<TaskStatus> finishedTaskpackages = new ArrayList<>();
        for (TaskStatus ts : taskStatus) {
            if (ts.getTaskStatus() == TaskStatus.Code.COMPLETE || ts.getTaskStatus() == TaskStatus.Code.ABORTED) {
                finishedTaskpackages.add(ts);
            }
        }
        return finishedTaskpackages;
    }

    private List<Integer> filterTargetReferencesFromTaskStatusList(List<TaskStatus> taskStatus) {
        List<Integer> targetReferences = new ArrayList<>();
        for (TaskStatus ts : taskStatus) {
            targetReferences.add(ts.getTargetReference());
        }
        return targetReferences;
    }

    private List<EsInFlight> getEsInFlightsFromTargetReferences(Map<Integer, EsInFlight> esInFlightMap, List<Integer> finishedTargetReferences) {
        List<EsInFlight> finishedEsInFlight = new ArrayList<>();
        for (Integer targetReference : finishedTargetReferences) {
            finishedEsInFlight.add(esInFlightMap.get(targetReference));
        }
        return finishedEsInFlight;
    }
}
