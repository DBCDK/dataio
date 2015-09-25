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

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.service.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.sink.es.entity.inflight.EsInFlight;
import dk.dbc.dataio.sink.types.SinkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static dk.dbc.dataio.commons.utils.lang.StringUtil.asBytes;

@MessageDriven
public class EsMessageProcessorBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(EsMessageProcessorBean.class);

    private AddiRecordPreprocessor addiRecordPreprocessor;

    @EJB
    EsConnectorBean esConnector;

    @EJB
    EsInFlightBean esInFlightAdmin;

    @EJB
    EsSinkConfigurationBean configuration;

    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    JSONBContext jsonbContext = new JSONBContext();

    @PostConstruct
    public void setup() {
        addiRecordPreprocessor = new AddiRecordPreprocessor();
    }

    /**
     * Generates ES workload from given processor result, creates ES task package, and
     * marks it as being in-flight.
     * @param consumedMessage message containing chunk payload
     * @throws SinkException on any failure during workload processing
     * @throws InvalidMessageException if unable to unmarshall message payload to chunk
     */
    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws SinkException, InvalidMessageException {
        final ExternalChunk processedChunk = unmarshallPayload(consumedMessage);
        final EsWorkload workload = getEsWorkloadFromChunkResult(processedChunk);
        final ExternalChunk deliveredChunk = workload.getDeliveredChunk();

        try {
            if (workload.getAddiRecords().isEmpty()) {
                LOGGER.info("chunk {} of job {} contained no Addi records - sending result",
                        deliveredChunk.getChunkId(), deliveredChunk.getJobId());
                try {
                    jobStoreServiceConnectorBean.getConnector().addChunkIgnoreDuplicates(deliveredChunk, deliveredChunk.getJobId(), deliveredChunk.getChunkId());
                } catch (JobStoreServiceConnectorException e) {
                    if (e instanceof JobStoreServiceConnectorUnexpectedStatusCodeException) {
                        final JobError jobError = ((JobStoreServiceConnectorUnexpectedStatusCodeException) e).getJobError();
                        if (jobError != null) {
                            LOGGER.error("job-store returned error: {}", jobError.getDescription());
                        }
                    }
                }
            } else {
                final int targetReference = esConnector.insertEsTaskPackage(workload);
                final EsInFlight esInFlight = new EsInFlight();
                esInFlight.setResourceName(configuration.getEsResourceName());
                esInFlight.setJobId(deliveredChunk.getJobId());
                esInFlight.setChunkId(deliveredChunk.getChunkId());
                esInFlight.setRecordSlots(workload.getAddiRecords().size());
                esInFlight.setTargetReference(targetReference);
                esInFlight.setIncompleteDeliveredChunk(jsonbContext.marshall(deliveredChunk));
                esInFlightAdmin.addEsInFlight(esInFlight);

                LOGGER.info("Created ES task package with target reference {} for chunk {} of job {}",
                        targetReference, deliveredChunk.getChunkId(), deliveredChunk.getJobId());
            }
        } catch (Exception e) {
            throw new SinkException("Exception caught during workload processing", e);
        }
    }

    /**
     * Generates ES workload based on given chunk content.
     * <br/> All input ChunkItems with status SUCCESS containing valid Addi records are converted into ChunkItem placeholders with status SUCCESS in Delivered Chunk
     * <br/> All input ChunkItems with status SUCCESS containing invalid Addi records are converted into ChunkItems with status FAILURE in Delivered Chunk
     * <br/> All input ChunkItems with status IGNORE are converted into ChunkItems with status IGNORE in Delivered Chunk
     * <br/> All input ChunkItems with status FAILURE are converted into ChunkItems with status IGNORE in Delivered Chunk
     * @param processedChunk processor result
     * @return ES workload
     * @throws SinkException on unhandled ChunkItem status
     */
    EsWorkload getEsWorkloadFromChunkResult(ExternalChunk processedChunk) throws SinkException {
        final int numberOfItems = processedChunk.size();
        final List<AddiRecord> addiRecords = new ArrayList<>(numberOfItems);
        final ExternalChunk incompleteDeliveredChunk = new ExternalChunk(processedChunk.getJobId(), processedChunk.getChunkId(), ExternalChunk.Type.DELIVERED);
        incompleteDeliveredChunk.setEncoding(processedChunk.getEncoding());
        final StopWatch stopWatch = new StopWatch();

        for(ChunkItem chunkItem : processedChunk) {
            switch (chunkItem.getStatus()) {
                case SUCCESS:
                    try {
                        final List<AddiRecord> addiRecordsFromItem = getAddiRecords(chunkItem);
                        addiRecords.addAll(addiRecordsFromItem);
                        // We use the data property of the ChunkItem placeholder kept in the ES
                        // in-flight database to store the number of Addi records from the
                        // original record - this information is used by the EsCleanupBean
                        // when creating the resulting sink chunk.
                        incompleteDeliveredChunk.insertItem(new ChunkItem(
                                chunkItem.getId(), asBytes(Integer.toString(addiRecordsFromItem.size())), ChunkItem.Status.SUCCESS));
                    } catch (RuntimeException | IOException e) {
                        incompleteDeliveredChunk.insertItem(new ChunkItem(
                                chunkItem.getId(), asBytes(e.getMessage()), ChunkItem.Status.FAILURE));
                    } finally {
                        LOGGER.info("Operation took {} milliseconds", stopWatch.getElapsedTime());
                    }
                    break;
                case FAILURE:
                    incompleteDeliveredChunk.insertItem(new ChunkItem(
                            chunkItem.getId(), asBytes("Failed by processor"), ChunkItem.Status.IGNORE));
                    break;
                case IGNORE:
                    incompleteDeliveredChunk.insertItem(new ChunkItem(
                            chunkItem.getId(), asBytes("Ignored by processor"), ChunkItem.Status.IGNORE));
                    break;
                default:
                    throw new SinkException("Unknown chunk item state: " + chunkItem.getStatus().name());
            }
        }
        return new EsWorkload(incompleteDeliveredChunk, addiRecords,
                configuration.getEsUserId(), configuration.getEsPackageType(), configuration.getEsAction());
    }

    private List<AddiRecord> getAddiRecords(ChunkItem chunkItem) throws IllegalArgumentException, IOException {
        final List<AddiRecord> addiRecords = ESTaskPackageUtil.getAddiRecordsFromChunkItem(chunkItem);
        final List<AddiRecord> preprocessedAddiRecords = new ArrayList<>(addiRecords.size());
        for (AddiRecord addiRecord : addiRecords) {
            preprocessedAddiRecords.add(addiRecordPreprocessor.execute(addiRecord));
        }
        return preprocessedAddiRecords;
    }
}
