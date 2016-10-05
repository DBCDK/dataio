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
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.EsSinkConfig;
import dk.dbc.dataio.commons.types.ObjectFactory;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.service.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.sink.es.entity.es.TaskSpecificUpdateEntity;
import dk.dbc.dataio.sink.es.entity.inflight.EsInFlight;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.dataio.sink.util.AddiUtil;
import dk.dbc.log.DBCTrackedLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@MessageDriven
public class EsMessageProcessorBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(EsMessageProcessorBean.class);

    // Packaged scoped due to unit test
    AddiRecordPreprocessor addiRecordPreprocessor;
    FlowStoreServiceConnector flowStoreServiceConnector;
    long sinkId;
    long highestVersionSeen = 0;
    EsSinkConfig sinkConfig;

    @EJB
    EsConnectorBean esConnector;

    @EJB
    EsInFlightBean esInFlightAdmin;

    @EJB
    FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;

    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnectorBean;



    @PostConstruct
    public void setup() {
        addiRecordPreprocessor = new AddiRecordPreprocessor();
        flowStoreServiceConnector = flowStoreServiceConnectorBean.getConnector();
        sinkId = Long.parseLong(System.getenv().get(Constants.SINK_ID_ENV_VARIABLE));
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
        retrieveVersionSpecificConfigValue(consumedMessage);
        final Chunk processedChunk = unmarshallPayload(consumedMessage);
        final EsWorkload workload = EsWorkload.create(processedChunk, sinkConfig, addiRecordPreprocessor);
        final Chunk deliveredChunk = workload.getDeliveredChunk();

        try {
            if (workload.getAddiRecords().isEmpty()) {
                LOGGER.info("chunk {} of job {} contained no Addi records - sending result", deliveredChunk.getChunkId(), deliveredChunk.getJobId());
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
                final int targetReference = esConnector.insertEsTaskPackage(workload, sinkConfig);
                final EsInFlight esInFlight = esInFlightAdmin.buildEsInFlight(deliveredChunk, targetReference, sinkConfig.getDatabaseName(), 0, sinkId);
                esInFlightAdmin.addEsInFlight(esInFlight);
                LOGGER.info("Created ES task package with target reference {} for chunk {} of job {}", targetReference, deliveredChunk.getChunkId(), deliveredChunk.getJobId());
            }
        } catch (Exception e) {
            throw new SinkException("Exception caught during workload processing", e);
        }
    }

    /**
     * This method determines if the es sink config is the latest version.
     *
     * If the current config is outdated:
     * The latest version of the ES sink config is retrieved through the referenced sink.

     * @param consumedMessage consumed message containing the current sink version
     * @throws SinkException on error to retrieve version from consumed message or on error when fetching sink
     */
    private void retrieveVersionSpecificConfigValue(ConsumedMessage consumedMessage) throws SinkException {
        try {
            final long sinkVersion = consumedMessage.getHeaderValue(JmsConstants.SINK_VERSION_PROPERTY_NAME, Long.class);

            LOGGER.trace("Sink version of message {} vs highest version seen {}", sinkVersion, highestVersionSeen);
            if (sinkVersion > highestVersionSeen) {
                final Sink sink = flowStoreServiceConnector.getSink(sinkId);
                sinkConfig = (EsSinkConfig) sink.getContent().getSinkConfig();
                highestVersionSeen = sink.getVersion();
                LOGGER.info("Current config values: {}, {}, {}",
                        sinkConfig.getUserId(),
                        sinkConfig.getDatabaseName(),
                        TaskSpecificUpdateEntity.UpdateAction.valueOf(sinkConfig.getEsAction()));

            }
        } catch (FlowStoreServiceConnectorException e) {
            throw new SinkException(e.getMessage(), e);
        }
    }
}
