package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.SinkChunkResult;
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
            List<SinkChunkResult> sinkChunkResults = createSinkChunkResults(finishedEsInFlight);
            esConnector.deleteESTaskpackages(finishedTargetReferences);
            removeEsInFlights(finishedEsInFlight);
            jobProcessorMessageProducer.sendAll(sinkChunkResults);
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

    private List<SinkChunkResult> createSinkChunkResults(List<EsInFlight> finshedEsInFlight) throws SinkException {
        List<SinkChunkResult> sinkChunkResults = new ArrayList<>(finshedEsInFlight.size());
        for (EsInFlight esInFlight : finshedEsInFlight) {
            sinkChunkResults.add(createSinkChunkResult(esInFlight));
        }
        return sinkChunkResults;
    }

    SinkChunkResult createSinkChunkResult(EsInFlight esInFlight) throws SinkException {
        LOGGER.info("Finishing SinkChunkResult for tp: {}", esInFlight.getTargetReference());
        final SinkChunkResult sinkChunkResult;
        try {
            sinkChunkResult = JsonUtil.fromJson(esInFlight.getSinkChunkResult(), SinkChunkResult.class);
        } catch (JsonException e) {
            throw new SinkException(String.format("Unable to marshall SinkChunkResult for tp: %d", esInFlight.getTargetReference()), e);
        }

        final List<ChunkItem> items = esConnector.getSinkResultItemsForTaskPackage(esInFlight.getTargetReference());
        final List<ChunkItem> resultItems = sinkChunkResult.getItems();

        // Fill out empty slots in pre-built SinkChunkResult
        int j = 0;
        for (int i = 0; i < resultItems.size(); i++) {
            if (resultItems.get(i).getStatus() == ChunkItem.Status.SUCCESS) {
                try {
                    resultItems.set(i, new ChunkItem(i + 1, items.get(j).getData(), items.get(j).getStatus()));
                    j++;
                } catch (IndexOutOfBoundsException e) {
                    throw new SinkException(String.format("SinkChunkResult item discrepancy for tp<%d>: %d items returned, more expected",
                    esInFlight.getTargetReference(), items.size()));
                }
            }
        }

        if (items.size() != j) {
            throw new SinkException(String.format("SinkChunkResult item discrepancy for tp<%d>: %d items returned, %d items used",
                    esInFlight.getTargetReference(), items.size(), j));
        }

        return sinkChunkResult;
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
