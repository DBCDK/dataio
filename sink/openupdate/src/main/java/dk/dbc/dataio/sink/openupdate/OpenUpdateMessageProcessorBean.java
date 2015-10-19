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

package dk.dbc.dataio.sink.openupdate;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.types.interceptor.Stopwatch;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.service.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.sink.types.SinkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.MessageDriven;

import static dk.dbc.dataio.commons.utils.lang.StringUtil.asBytes;

@MessageDriven
public class OpenUpdateMessageProcessorBean extends AbstractSinkMessageConsumerBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenUpdateMessageProcessorBean.class);

    @EJB JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    @Stopwatch
    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws ServiceException, InvalidMessageException {
        final ExternalChunk processedChunk = unmarshallPayload(consumedMessage);
        LOGGER.info("External Chunk received successfully. Chunk ID: " + processedChunk.getChunkId() + ", Job ID: " + processedChunk.getJobId());

        final ExternalChunk chunkForDelivery = buildBasicDeliveredChunkFromProcessedChunk(processedChunk);

        if(processedChunk.isEmpty()) {

            LOGGER.info("OpenUpdate Sink received chunk {} of job {} with no Addi records - sending result", chunkForDelivery.getChunkId(), chunkForDelivery.getJobId());
            addChunkInJobStore(chunkForDelivery);
        } else {

//            final List<AddiRecord> addiRecords = new ArrayList<>(processedChunk.size());
            // Call the OpenUpdate web service for each ChunkItem if processed successfully.
            for(ChunkItem processedChunkItem : processedChunk) {

                switch (processedChunkItem.getStatus()) {

                    case SUCCESS:   callOpenUpdateWebServiceAndAddChunkItem(chunkForDelivery, processedChunkItem);                              break;

                    case FAILURE:   chunkForDelivery.addItemWithStatusIgnored(processedChunkItem.getId(), asBytes("Failed by processor"));   break;

                    case IGNORE:    chunkForDelivery.addItemWithStatusIgnored(processedChunkItem.getId(), asBytes("Ignored by processor"));  break;

                    default:        throw new SinkException("Unknown chunk item state: " + processedChunkItem.getStatus().name());
                }
            }

            addChunkInJobStore(chunkForDelivery);
        }
    }

    private void callOpenUpdateWebServiceAndAddChunkItem(ExternalChunk chunkForDelivery, ChunkItem processedChunkItem) {
        try {
            //final List<AddiRecord> addiRecordsFromItem = getAddiRecords(chunkItem);
//                            addiRecords.addAll(addiRecordsFromItem);


            // We use the data property of the ChunkItem placeholder kept in the ES
            // in-flight database to store the number of Addi records from the
            // original record - this information is used by the EsCleanupBean
            // when creating the resulting sink chunk.



            chunkForDelivery.addItemWithStatusSuccess(processedChunkItem.getId(), asBytes("Suuuuccess =D"));
        } catch (RuntimeException /*| IOException */ e) {
            chunkForDelivery.addItemWithStatusFailed(processedChunkItem.getId(), asBytes(e.getMessage()));
        }
    }

    private void addChunkInJobStore(ExternalChunk chunkForDelivery) {
        try {
            jobStoreServiceConnectorBean.getConnector().addChunkIgnoreDuplicates(chunkForDelivery, chunkForDelivery.getJobId(), chunkForDelivery.getChunkId());
        } catch (JobStoreServiceConnectorException e) {
            if (e instanceof JobStoreServiceConnectorUnexpectedStatusCodeException) {
                final JobError jobError = ((JobStoreServiceConnectorUnexpectedStatusCodeException) e).getJobError();
                if (jobError != null) {
                    LOGGER.error("job-store returned error: {}", jobError.getDescription());
                }
            }
        }
    }

    private ExternalChunk buildBasicDeliveredChunkFromProcessedChunk(ExternalChunk processedChunk) {
        final ExternalChunk incompleteDeliveredChunk = new ExternalChunk(processedChunk.getJobId(), processedChunk.getChunkId(), ExternalChunk.Type.DELIVERED);
        incompleteDeliveredChunk.setEncoding(processedChunk.getEncoding());
        return incompleteDeliveredChunk;
    }
/*
    private ExternalChunk mapWebServiceResultToExternalChunk(UpdateRecordResult updateRecordResult) {
        return null;
    }

    private UpdateRecordResult callWebService() {
        AddiRecordPreprocessor addiRecordPreprocessor = new AddiRecordPreprocessor();
        return addiRecordPreprocessor.execute(new AddiRecord(new byte[0], new byte[0]));
    }
*/
}