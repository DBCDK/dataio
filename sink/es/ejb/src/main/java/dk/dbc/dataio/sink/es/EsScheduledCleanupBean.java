package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.sink.SinkException;
import dk.dbc.dataio.sink.es.ESTaskPackageUtil.TaskStatus;
import dk.dbc.dataio.sink.es.entity.EsInFlight;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@LocalBean
@Singleton
@Startup
@DependsOn("EsThrottlerBean")
public class EsScheduledCleanupBean {

    private static final XLogger LOGGER = XLoggerFactory.getXLogger(EsScheduledCleanupBean.class);

    @EJB
    EsConnectorBean esConnector;

    @EJB
    EsInFlightBean esInFlightAdmin;

    @EJB
    EsThrottlerBean esThrottler;

    @PostConstruct
    public void startup() {
        LOGGER.info("Startup of EsScheduledCleanupBean!");
        List<EsInFlight> esInFlightList = esInFlightAdmin.listEsInFlight();
        LOGGER.info("The following targetrefernces are inFlight in the sink at startup: {}",
                Arrays.toString( createEsInFlightMap(esInFlightList).keySet().toArray()) );
        int slotsInFlight = sumRecordSlotsInEsInFlightList(esInFlightList);
        LOGGER.info("Sum of recordSlots for inFlight Chunks: [{}]", slotsInFlight);
        try {
            esThrottler.acquireRecordSlots(slotsInFlight);
        } catch(IllegalArgumentException | InterruptedException e) {
            LOGGER.error("An exception was caught while trying to count down the esThrotler: {}", e.getMessage(), e);
        }
        // Integrity-test is deffered to the first run of cleanup().
    }

    private int sumRecordSlotsInEsInFlightList(List<EsInFlight> esInFlightList) {
        int res = 0;
        for(EsInFlight esInFlight : esInFlightList) {
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
     */
    @Schedule(second = "*/30", minute = "*", hour = "*", persistent = false)
    public void cleanup() {
        LOGGER.info("Cleaning up ES-base");
        try {
            Map<Integer, EsInFlight> esInFlightMap = createEsInFlightMap(esInFlightAdmin.listEsInFlight());
            if (esInFlightMap.isEmpty()) {
                LOGGER.info("No records in ES InFlight.");
                return;
            }
            List<Integer> targetReferences = new ArrayList<>(esInFlightMap.keySet());
            List<TaskStatus> taskStatus = esConnector.getCompletionStatusForESTaskpackages(targetReferences);
            validateTaskStatusVsTargetReference(taskStatus, esInFlightMap);
            List<TaskStatus> finishedTaskpackages = filterFinsihedTaskpackages(taskStatus);
            List<Integer> finishedTargetReferences = filterTargetReferencesFromTaskStatusList(finishedTaskpackages);
            if (finishedTargetReferences.isEmpty()) {
                LOGGER.info("No finished taskpackages ES.");
                return;
            }
            esConnector.deleteESTaskpackages(finishedTargetReferences);
            List<EsInFlight> finishedEsInFlight = getEsInFlightsFromTargetReferences(esInFlightMap, finishedTargetReferences);
            int recordSlotsToRelease = 0;
            for (EsInFlight esInFlight : finishedEsInFlight) {
                recordSlotsToRelease += esInFlight.getRecordSlots();
                esInFlightAdmin.removeEsInFlight(esInFlight);
            }

            // todo: Missing implementation of "callback" to jobhandler of finished chunks with results.
            esThrottler.releaseRecordSlots(recordSlotsToRelease);
        } catch (SinkException ex) {
            LOGGER.error("A SinkExceptin was thrown during cleanup of ES/inFlight: {}", ex.getMessage(), ex);
        }
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
