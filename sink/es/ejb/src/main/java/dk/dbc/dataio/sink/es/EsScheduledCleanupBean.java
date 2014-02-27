package dk.dbc.dataio.sink.es;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.sink.SinkException;
import dk.dbc.dataio.sink.es.ESTaskPackageUtil.TaskStatus;
import dk.dbc.dataio.sink.es.entity.EsInFlight;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.TextMessage;
import java.nio.charset.Charset;
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

    private static final String SINK_CHUNK_RESULT_MESSAGE_PROPERTY_NAME = "chunkResultSource";
    private static final String SINK_CHUNK_RESULT_MESSAGE_PROPERTY_VALUE = "sink";

    @EJB
    EsConnectorBean esConnector;

    @EJB
    EsInFlightBean esInFlightAdmin;

    @EJB
    EsThrottlerBean esThrottler;

    @EJB
    EsSinkConfigurationBean configuration;

    @Resource
    ConnectionFactory jobProcessorQueueConnectionFactory;

    @Resource(name = "jobProcessorJmsQueue") // this resource gets its jndi name mapping from xml-deploy-descriptors
    Queue jobProcessorJmsQueue;

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
            List<Integer> finishedTargetReferences = findTargetReferencesForCompletedTaskpackagesFromEsInFightMap(esInFlightMap);
            if (finishedTargetReferences.isEmpty()) {
                LOGGER.info("No finished taskpackages in ES.");
                return;
            }
            esConnector.deleteESTaskpackages(finishedTargetReferences);
            List<EsInFlight> finishedEsInFlight = getEsInFlightsFromTargetReferences(esInFlightMap, finishedTargetReferences);
            int recordSlotsToRelease = sumRecordSlotsInEsInFlightList(finishedEsInFlight);
            List<SinkChunkResult> sinkChunkResults = createSinkChunkResults(finishedEsInFlight);
            removeEsInFlights(finishedEsInFlight);
            sendSinkResultsToJobProcessor(sinkChunkResults);
            esThrottler.releaseRecordSlots(recordSlotsToRelease);
        } catch (SinkException ex) {
            LOGGER.error("A SinkException was thrown during cleanup of ES/inFlight", ex);
        }
    }

    private void sendSinkResultsToJobProcessor(List<SinkChunkResult> sinkChunkResults) {
        try (JMSContext context = jobProcessorQueueConnectionFactory.createContext()) {
            for (SinkChunkResult sinkChunkResult : sinkChunkResults) {
                final TextMessage message = context.createTextMessage(JsonUtil.toJson(sinkChunkResult));
                message.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.SINK_RESULT_PAYLOAD_TYPE);
                message.setStringProperty(SINK_CHUNK_RESULT_MESSAGE_PROPERTY_NAME, SINK_CHUNK_RESULT_MESSAGE_PROPERTY_VALUE);
                context.createProducer().send(jobProcessorJmsQueue, message);
            }
        } catch (JsonException | JMSException e) {
            throw new EJBException(e);
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

    private List<SinkChunkResult> createSinkChunkResults(List<EsInFlight> finshedEsInFlight) {
        List<SinkChunkResult> sinkChunkResults = new ArrayList<>(finshedEsInFlight.size());
        for (EsInFlight esInFlight : finshedEsInFlight) {
            sinkChunkResults.add(createSinkChunkResult(esInFlight));
        }
        return sinkChunkResults;
    }

    private SinkChunkResult createSinkChunkResult(EsInFlight esInFlight) {
        // This is where the SinkChunkResult should fetch status and data from the taskpackage.
        final List<ChunkItem> items = new ArrayList<>();
        items.add(new ChunkItem(0, "", ChunkItem.Status.SUCCESS));
        return new SinkChunkResult(esInFlight.getJobId(), esInFlight.getChunkId(), Charset.defaultCharset(), items);
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
