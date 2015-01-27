package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.sink.es.ESTaskPackageUtil.TaskStatus;
import dk.dbc.dataio.sink.es.entity.EsInFlight;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.dataio.sink.utils.messageproducer.JobProcessorMessageProducerBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
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
    JobProcessorMessageProducerBean jobProcessorMessageProducer;

    @PostConstruct
    public void startup() {
        LOGGER.info("Startup of EsScheduledCleanupBean!");
        List<EsInFlight> esInFlightList = esInFlightAdmin.listEsInFlight();
        LOGGER.info("The following targetreferences are inFlight in the sink at startup: {}",
                Arrays.toString(createEsInFlightMap(esInFlightList).keySet().toArray()));
        int slotsInFlight = sumRecordSlotsInEsInFlightList(esInFlightList);
        LOGGER.info("Sum of recordSlots for inFlight Chunks: [{}]", slotsInFlight);
        try {
            esThrottler.acquireRecordSlots(slotsInFlight);
        } catch (IllegalArgumentException | InterruptedException e) {
            LOGGER.error("An exception was caught while trying to count down the esThrotler: {}", e.getMessage(), e);
        }
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
        LOGGER.info("Cleaning up ES-base");
        try {
            Map<Integer, EsInFlight> esInFlightMap = createEsInFlightMap(esInFlightAdmin.listEsInFlight());
            if (esInFlightMap.isEmpty()) {
                LOGGER.info("No records in ES InFlight.");
                return;
            }
            List<Integer> finishedTargetReferences = findTargetReferencesForCompletedTaskpackagesFromEsInFightMap(esInFlightMap);
            if (finishedTargetReferences.isEmpty()) {
                LOGGER.info("No finished taskpackages in ES.");
                return;
            }
            List<EsInFlight> finishedEsInFlight = getEsInFlightsFromTargetReferences(esInFlightMap, finishedTargetReferences);
            int recordSlotsToRelease = sumRecordSlotsInEsInFlightList(finishedEsInFlight);
            List<ExternalChunk> deliveredChunks = createDeliveredChunks(finishedEsInFlight);
            esConnector.deleteESTaskpackages(finishedTargetReferences);
            removeEsInFlights(finishedEsInFlight);
            jobProcessorMessageProducer.sendAll(deliveredChunks);
            esThrottler.releaseRecordSlots(recordSlotsToRelease);
        } catch (SinkException ex) {
            LOGGER.error("A SinkException was thrown during cleanup of ES/inFlight", ex);
        }
    }

    private void removeEsInFlights(List<EsInFlight> esInFlightList) {
        for (EsInFlight esInFlight : esInFlightList) {
            esInFlightAdmin.removeEsInFlight(esInFlight);
        }
    }

    private List<Integer> findTargetReferencesForCompletedTaskpackagesFromEsInFightMap(Map<Integer, EsInFlight> esInFlightMap) throws SinkException {
        List<Integer> targetReferences = new ArrayList<>(esInFlightMap.keySet());
        List<TaskStatus> taskStatus = esConnector.getCompletionStatusForESTaskpackages(targetReferences);
        validateTaskStatusVsTargetReference(taskStatus, esInFlightMap);
        List<TaskStatus> finishedTaskpackages = filterFinsihedTaskpackages(taskStatus);
        return filterTargetReferencesFromTaskStatusList(finishedTaskpackages);
    }

    private List<ExternalChunk> createDeliveredChunks(List<EsInFlight> finshedEsInFlight) throws SinkException {
        List<ExternalChunk> deliveredChunks = new ArrayList<>(finshedEsInFlight.size());
        for (EsInFlight esInFlight : finshedEsInFlight) {
            deliveredChunks.add(createDeliveredChunk(esInFlight));
        }
        return deliveredChunks;
    }

    ExternalChunk createDeliveredChunk(EsInFlight esInFlight) throws SinkException {
        LOGGER.info("Finishing delivered chunk for tp: {}", esInFlight.getTargetReference());
        final ExternalChunk incompleteDeliveredChunk;
        try {
            incompleteDeliveredChunk = JsonUtil.fromJson(esInFlight.getIncompleteDeliveredChunk(), ExternalChunk.class);
        } catch (JsonException e) {
            throw new SinkException(String.format("Unable to marshall delivered chunk for tp: %d", esInFlight.getTargetReference()), e);
        }

        // this list only contains the resulting items for the successfull items in the incompleteDeliveredChunk.
        final List<ChunkItem> items = esConnector.getResultingItemsFromSinkForTaskPackage(esInFlight.getTargetReference());
        
        // Ensure that all items from the taskpackage has a match in the incompleteDeliveredChunk,
        // i.e. the number of successfull items in incompleteDeliveredChunk must equals the number of
        // items from the taskpackage
        int successfullItemsFromIncompleteDeliveredChunk = getNumberOfSuccessfullItemsFromIncompleteDeliveredChunk(incompleteDeliveredChunk);
        throwIfMismatchBewteenNumberOfitems(items, successfullItemsFromIncompleteDeliveredChunk, esInFlight, incompleteDeliveredChunk);
        ExternalChunk deliveredChunk = weaveItemsFromTaskpackageAndIncompleteDeliveredChunk(incompleteDeliveredChunk, items, esInFlight);        

        return deliveredChunk;
    }
    
    private int getNumberOfSuccessfullItemsFromIncompleteDeliveredChunk(final ExternalChunk incompleteDeliveredChunk) {
        int successfullItemsFromIncompleteDeliveredChunk = 0;
        for(ChunkItem item : incompleteDeliveredChunk) {
            successfullItemsFromIncompleteDeliveredChunk += item.getStatus() == ChunkItem.Status.SUCCESS ? 1 : 0;
        }
        return successfullItemsFromIncompleteDeliveredChunk;
    }

    private void throwIfMismatchBewteenNumberOfitems(final List<ChunkItem> items, int successfullItemsFromIncompleteDeliveredChunk, EsInFlight esInFlight, final ExternalChunk incompleteDeliveredChunk) throws SinkException {
        if (items.size() != successfullItemsFromIncompleteDeliveredChunk) {
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

    private void validateTaskStatusVsTargetReference(List<TaskStatus> taskStatus, Map<Integer, EsInFlight> esInFlightMap) {
        // todo: It is not totally clear what should happen here ...
        //       If a targetreference which is present in EsInFlight is missing in the ES-base,
        //       then that particular job should be aborted??? Or should we just keep on going???
        //       Or should we set up some mean for adding the taskpackage again and retry???
        LOGGER.info("Method not yet implemented!");
        LOGGER.info("Number of [taskstatus/esInFlight] : [{}/{}]", taskStatus.size(), esInFlightMap.size());
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
