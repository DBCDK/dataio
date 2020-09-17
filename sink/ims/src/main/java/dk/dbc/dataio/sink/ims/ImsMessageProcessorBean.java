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

package dk.dbc.dataio.sink.ims;

import dk.dbc.commons.metricshandler.MetricsHandlerBean;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ImsSinkConfig;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.sink.types.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.sink.ims.connector.ImsServiceConnector;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.xml.ws.WebServiceException;
import java.time.Duration;
import java.util.List;

@MessageDriven
public class ImsMessageProcessorBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImsMessageProcessorBean.class);

    @EJB JobStoreServiceConnectorBean jobStoreServiceConnectorBean;
    @EJB ImsConfigBean imsConfigBean;

    final MarcXchangeRecordUnmarshaller marcXchangeRecordUnmarshaller = new MarcXchangeRecordUnmarshaller();

    private ImsSinkConfig config;
    private ImsServiceConnector connector;

    @Inject
    MetricsHandlerBean metricsHandler;

    @Stopwatch
    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws SinkException, InvalidMessageException, NullPointerException, WebServiceException {
        try {
            final Chunk chunk = unmarshallPayload(consumedMessage);
            LOGGER.info("Received chunk {}/{}", chunk.getJobId(), chunk.getChunkId());

            final ImsSinkConfig latestConfig = imsConfigBean.getConfig(consumedMessage);
            if(!latestConfig.equals(config)) {
                LOGGER.debug("Updating connector");
                connector = getImsServiceConnector(latestConfig);
                config = latestConfig;
            }

            try {
                long imsRequestStartTime = System.currentTimeMillis();
                final SinkResult sinkResult = new SinkResult(chunk, marcXchangeRecordUnmarshaller);
                if(!sinkResult.getMarcXchangeRecords().isEmpty()) {
                    final List<UpdateMarcXchangeResult> marcXchangeResults = connector.updateMarcXchange(
                            String.format("%d-%d", chunk.getJobId(), chunk.getChunkId()), sinkResult.getMarcXchangeRecords());
                    sinkResult.update(marcXchangeResults);
                }
                metricsHandler.update(ImsTimerMetrics.REQUEST_DURATION, Duration.ofMillis(System.currentTimeMillis() - imsRequestStartTime));
                metricsHandler.increment(ImsCounterMetrics.REQUESTS);
                addChunkToJobStore(sinkResult.toChunk());
            } catch(WebServiceException e) {
                LOGGER.error("WebServiceException caught when handling chunk {}/{}", chunk.getJobId(), chunk.getChunkId(), e);
                metricsHandler.increment(ImsCounterMetrics.IMS_FAILURES);
                throw e;
            }
        }
        catch( WebServiceException we ) {
            // Rethrow, exception has been handled in inner catch and should not be counted as unhandled
            throw we;
        }
        catch( Exception e ) {
            LOGGER.error("Caught unhandled exception {}", e);
            metricsHandler.increment(ImsCounterMetrics.UNHANDLED_EXCEPTIONS);
            throw e;
        }
    }

    private ImsServiceConnector getImsServiceConnector(ImsSinkConfig config) {
        return new ImsServiceConnector(config.getEndpoint());
    }

    private void addChunkToJobStore(Chunk outcome) throws SinkException {
        try {
            jobStoreServiceConnectorBean.getConnector().addChunkIgnoreDuplicates(outcome, outcome.getJobId(), outcome.getChunkId());
        } catch (JobStoreServiceConnectorException e) {
            logJobStoreError(e);
            // Throw SinkException to force transaction rollback
            throw new SinkException("Error in communication with job-store", e);
        }
    }

    private void logJobStoreError(JobStoreServiceConnectorException e) {
        if (e instanceof JobStoreServiceConnectorUnexpectedStatusCodeException) {
            final JobError jobError = ((JobStoreServiceConnectorUnexpectedStatusCodeException) e).getJobError();
            if (jobError != null) {
                LOGGER.error("job-store returned error: {}", jobError.getDescription());
            }
        }
    }
}
