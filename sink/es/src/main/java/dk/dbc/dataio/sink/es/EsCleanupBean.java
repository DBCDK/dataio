/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.sink.es;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.EsSinkConfig;
import dk.dbc.dataio.commons.types.ObjectFactory;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.sink.es.ESTaskPackageUtil.TaskStatus;
import dk.dbc.dataio.sink.es.entity.es.TaskPackageEntity;
import dk.dbc.dataio.sink.es.entity.inflight.EsInFlight;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.log.DBCTrackedLogContext;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@LocalBean
@Singleton
public class EsCleanupBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(EsCleanupBean.class);
    final static int MAX_REDELIVERING_ATTEMPTS = 10;
    AddiRecordPreprocessor addiRecordPreprocessor;

    @EJB
    EsConnectorBean esConnector;

    @EJB
    EsInFlightBean esInFlightAdmin;

    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    @EJB
    FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;

    // Packaged scoped due to unit test
    JSONBContext jsonbContext;
    JobStoreServiceConnector jobStoreServiceConnector;
    FlowStoreServiceConnector flowStoreServiceConnector;
    long sinkId;

    @PostConstruct
    public void startup() {
        jsonbContext = new JSONBContext();
        addiRecordPreprocessor = new AddiRecordPreprocessor();
        jobStoreServiceConnector = jobStoreServiceConnectorBean.getConnector();
        flowStoreServiceConnector = flowStoreServiceConnectorBean.getConnector();
        sinkId = Long.parseLong(System.getenv().get(Constants.SINK_ID_ENV_VARIABLE));
        LOGGER.info("Startup of EsScheduledCleanupBean!");
        LOGGER.info("The following targetreferences are inFlight in the sink at startup: {}",
                Arrays.toString(createEsInFlightMap().keySet().toArray()));
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
            final Map<Integer, EsInFlight> esInFlightMap = createEsInFlightMap();
            if (esInFlightMap.isEmpty()) {
                LOGGER.info("No records in ES InFlight.");
                return;
            }
            final Map<Integer, TaskStatus> taskStatusMap = getTaskStatusForESTaskpackages(esInFlightMap);

            final List<Chunk> lostChunks = findLostChunks(esInFlightMap, taskStatusMap);
            if(!lostChunks.isEmpty()) {
                addChunks(lostChunks);
            }

            final List<Chunk> deliveredChunks = findDeliveredChunks(esInFlightMap, taskStatusMap);
            if(!deliveredChunks.isEmpty()) {
                addChunks(deliveredChunks);
            }

        } catch (SinkException ex) {
            LOGGER.error("A SinkException was thrown during cleanup of ES/inFlight", ex);
        } finally {
            LOGGER.info("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    private void addChunks(List<Chunk> chunks) {
        JobStoreServiceConnector jobStoreServiceConnector = jobStoreServiceConnectorBean.getConnector();
        for(Chunk chunk : chunks) {
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
            if (ts.getTaskStatus() == TaskPackageEntity.TaskStatus.COMPLETE || ts.getTaskStatus() == TaskPackageEntity.TaskStatus.ABORTED) {
                finishedTaskPackages.add(entry.getKey());
            }
        }
        return finishedTaskPackages;
    }

    private Map<Integer, TaskStatus> getTaskStatusForESTaskpackages(Map<Integer, EsInFlight> esInFlightMap) throws SinkException {
        return esConnector.getCompletionStatusForESTaskpackages(new ArrayList<>(esInFlightMap.keySet()));
    }

    private List<Chunk> findLostChunks(Map<Integer, EsInFlight> esInFlightMap, Map<Integer, TaskStatus> taskStatusMap) throws SinkException {
        final StopWatch stopWatch = new StopWatch();
        try {
            List<Chunk> lostChunks = new ArrayList<>();
            List<EsInFlight> lostEsInFlight = new ArrayList<>();
            LOGGER.info("Number of [taskstatus/esInFlight] : [{}/{}]", taskStatusMap.size(), esInFlightMap.size());

            for (Map.Entry<Integer, EsInFlight> esInFlightEntry : esInFlightMap.entrySet()) {
                EsInFlight currentEsInFlight = esInFlightEntry.getValue();

                if (!taskStatusMap.containsKey(esInFlightEntry.getKey())) {
                    if (currentEsInFlight.getRedelivered() == MAX_REDELIVERING_ATTEMPTS) {
                        lostChunks.add(createLostChunk(esInFlightEntry.getValue()));
                        lostEsInFlight.add(currentEsInFlight);
                        LOGGER.info("{} unsuccessful redelivering attempts. Lost chunk created for chunk {} in job {}",
                                MAX_REDELIVERING_ATTEMPTS, currentEsInFlight.getChunkId(), currentEsInFlight.getJobId());
                    } else {
                        LOGGER.info("Preparing to redeliver due to missing task package for chunk {} in job {}",
                                currentEsInFlight.getChunkId(), currentEsInFlight.getJobId());

                        redeliver(currentEsInFlight);
                    }
                }
            }
            if (!lostEsInFlight.isEmpty()) {
                LOGGER.info("Number of lost taskpackages in ES : {}.", esInFlightMap.size());
                removeEsInFlights(lostEsInFlight);
            }
            return lostChunks;
        } finally {
            LOGGER.info("Operation took {} milliseconds", stopWatch.getElapsedTime());
        }
    }

    /**
     * Redelivers processed chunk referenced by the EsInFlight given as input
     * @param currentEsInFlight the EsInFlight needed to create target reference and processed chunk
     * @throws SinkException on any failure
     */
    private void redeliver(EsInFlight currentEsInFlight) throws SinkException {
        try {
            final int redelivered = currentEsInFlight.getRedelivered() + 1;
            final int jobId = currentEsInFlight.getJobId().intValue();
            final int chunkId = currentEsInFlight.getChunkId().intValue();
            final short numberOfItems = (short)(jsonbContext.unmarshall(currentEsInFlight.getIncompleteDeliveredChunk(), Chunk.class).size());

            // Rebuild processed chunk
            final Chunk processedChunk = new Chunk(currentEsInFlight.getJobId(), currentEsInFlight.getChunkId(), Chunk.Type.PROCESSED);

            for (int i = 0; i < numberOfItems; i ++) {
                processedChunk.insertItem(jobStoreServiceConnector.getChunkItem(jobId, chunkId, numberOfItems, State.Phase.PROCESSING));
            }

            final EsSinkConfig sinkConfig = (EsSinkConfig) flowStoreServiceConnectorBean.getConnector().getSink(sinkId).getContent().getSinkConfig();
            final EsWorkload workload = EsWorkload.create(processedChunk, sinkConfig, addiRecordPreprocessor);

            // Insert esTaskPackage
            final int targetReference = esConnector.insertEsTaskPackage(workload, sinkConfig);

            // Remove existing and add new esInFlight
            esInFlightAdmin.removeEsInFlight(currentEsInFlight);
            final EsInFlight newEsInFlight = esInFlightAdmin.buildEsInFlight(processedChunk, targetReference, sinkConfig.getDatabaseName(), redelivered, sinkId);
            esInFlightAdmin.addEsInFlight(newEsInFlight);

            LOGGER.info("{}. redelivering completed. Created ES task package with target reference {}", redelivered, targetReference);
        } catch (Exception e) {
            throw new SinkException("Exception occurred while redelivering chunk", e);
        }
    }

    Chunk createLostChunk(EsInFlight esInFlight) throws SinkException {
        LOGGER.info("Finishing lost chunk for tp: {}", esInFlight.getTargetReference());
        final Chunk chunk;
        try {
            chunk = jsonbContext.unmarshall(esInFlight.getIncompleteDeliveredChunk(), Chunk.class);
        } catch (JSONBException e) {
            throw new SinkException(String.format("Unable to marshall lost chunk for tp: %d", esInFlight.getTargetReference()), e);
        }

        // Create a new chunk representing the lost target reference
        final Chunk lostChunk = new Chunk(chunk.getJobId(), chunk.getChunkId(), chunk.getType());
        lostChunk.setEncoding(chunk.getEncoding());

        try {
            for (ChunkItem chunkItem : chunk) {
                DBCTrackedLogContext.setTrackingId(chunkItem.getTrackingId());
                LOGGER.info("Handling item {} for lost chunk {} in job {}", chunkItem.getId(), chunk.getChunkId(), chunk.getJobId());
                lostChunk.insertItem(createLostChunkItem(chunkItem));
            }
        } finally {
            DBCTrackedLogContext.remove();
        }
        return lostChunk;
    }

    private ChunkItem createLostChunkItem(ChunkItem chunkItem) {
        final ChunkItem lostChunkItem;
        if(chunkItem.getStatus() == ChunkItem.Status.SUCCESS) {
            final String data = "Item status set to failed due to taskpackage lost in ES";
            lostChunkItem = ObjectFactory.buildFailedChunkItem(chunkItem.getId(), data, ChunkItem.Type.STRING, chunkItem.getTrackingId());
            lostChunkItem.appendDiagnostics(ObjectFactory.buildFatalDiagnostic(data));
        } else {
            lostChunkItem = chunkItem;
        }
        return lostChunkItem;
    }

    private List<Chunk> findDeliveredChunks(Map<Integer, EsInFlight> esInFlightMap, Map<Integer, TaskStatus> taskStatusMap) {
        final StopWatch stopWatch = new StopWatch();
        List<Chunk> deliveredChunks = new ArrayList<>();
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

    private List<Chunk> createDeliveredChunks(List<EsInFlight> finishedEsInFlight) throws SinkException {
        List<Chunk> deliveredChunks = new ArrayList<>(finishedEsInFlight.size());
        for (EsInFlight esInFlight : finishedEsInFlight) {
            deliveredChunks.add(createDeliveredChunk(esInFlight));
        }
        return deliveredChunks;
    }

    Chunk createDeliveredChunk(EsInFlight esInFlight) throws SinkException {
        LOGGER.info("Finishing delivered chunk for tp: {}", esInFlight.getTargetReference());
        final Chunk incompleteDeliveredChunk;
        try {
            incompleteDeliveredChunk = jsonbContext.unmarshall(esInFlight.getIncompleteDeliveredChunk(), Chunk.class);
        } catch (JSONBException e) {
            throw new SinkException(String.format("Unable to marshall delivered chunk for tp: %d", esInFlight.getTargetReference()), e);
        }

        return esConnector.getChunkForTaskPackage(esInFlight.getTargetReference(), incompleteDeliveredChunk);
    }

    private Map<Integer, EsInFlight> createEsInFlightMap() {
        return esInFlightAdmin.listEsInFlight(sinkId).stream().collect(Collectors.toMap(EsInFlight::getTargetReference, c -> c));
    }


    private List<EsInFlight> getEsInFlightsFromTargetReferences(Map<Integer, EsInFlight> esInFlightMap, List<Integer> finishedTargetReferences) {
        return finishedTargetReferences.stream().map(esInFlightMap::get).collect(Collectors.toList());
    }
}
