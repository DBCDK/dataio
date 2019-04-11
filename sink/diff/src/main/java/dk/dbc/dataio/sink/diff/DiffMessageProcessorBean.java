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

package dk.dbc.dataio.sink.diff;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.ObjectFactory;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.sink.types.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.javascript.recordprocessing.FailRecord;
import dk.dbc.log.DBCTrackedLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

@MessageDriven
public class DiffMessageProcessorBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiffMessageProcessorBean.class);

    @EJB
    AddiDiffGenerator addiDiffGenerator;

    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    @EJB
    ExternalToolDiffGenerator externalToolDiffGenerator;

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws ServiceException, InvalidMessageException {
        final Chunk processedChunk = unmarshallPayload(consumedMessage);
        final Chunk deliveredChunk = processPayload(processedChunk);
        try {
            jobStoreServiceConnectorBean.getConnector().addChunkIgnoreDuplicates(deliveredChunk, deliveredChunk.getJobId(), deliveredChunk.getChunkId());
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

    /**
     * <br/> All 'current' input ChunkItems have their status compared with the status of their 'next' input ChunkItem counterpart.
     * <br/> If status differs, a ChunkItem placeholder is created with status FAILURE in Delivered Chunk
     * <br/>
     * <br/> If the status of 'current' and 'next input ChunkItem is identical:
     * <br/> All input ChunkItems with status IGNORE are converted into ChunkItem placeholders with status IGNORE in Delivered Chunk
     * <br/> All input ChunkItems with status FAILURE are converted into ChunkItem placeholders with status IGNORE in Delivered Chunk
     * <br/> All 'current' input ChunkItems, with status SUCCESS, have their data compared with the data of their 'next' input ChunkItem counterpart:
     * <br/>  - If the result is an empty diff String, the item is converted into a ChunkItem placeholder with status SUCCESS in Delivered Chunk
     * <br/>  - If the result is NOT an empty diff String, the item is converted into a ChunkItem placeholder with status FAILURE in Delivered Chunk
     * <br/>  - If a DiffGeneratorException is thrown, while comparing item data with next item data, the item is converted into a ChunkItem
     * <br/>    placeholder with status FAILURE in Delivered Chunk
     *
     * @param processedChunk processor result
     * @return processPayload
     * @throws SinkException on unhandled ChunkItem status
     */
    Chunk processPayload(Chunk processedChunk) throws SinkException{
        if( !processedChunk.hasNextItems()) {
            return failWithMissingNextItem(processedChunk);
        }

        final Chunk deliveredChunk = new Chunk(processedChunk.getJobId(), processedChunk.getChunkId(), Chunk.Type.DELIVERED);
        try {
            for (final ChunkItemPair item : buildCurrentNextChunkList(processedChunk)) {
                // TODO: 03/02/16 How does this and other "re-run job operations" influence on traceability?
                DBCTrackedLogContext.setTrackingId(item.current.getTrackingId());
                LOGGER.info("Handling item {} for chunk {} in job {}", item.current.getId(), processedChunk.getChunkId(), processedChunk.getJobId());
                if (item.current.getStatus() != item.next.getStatus()) {
                    String message = String.format("Different status %s -> %s\n%s",
                            statusToString(item.current.getStatus()),
                            statusToString(item.next.getStatus()),
                            StringUtil.asString(item.next.getData())
                    );
                    ChunkItem chunkItem = ObjectFactory.buildFailedChunkItem(item.current.getId(), message, ChunkItem.Type.STRING, item.current.getTrackingId());
                    chunkItem.appendDiagnostics(ObjectFactory.buildFatalDiagnostic(message));
                    deliveredChunk.insertItem(chunkItem);
                } else {
                    switch (item.current.getStatus()) {
                        case SUCCESS:
                            deliveredChunk.insertItem(getChunkItemWithDiffResult(item));
                            break;
                        case FAILURE:
                            deliveredChunk.insertItem(compareFailedItems(item));
                            break;
                        case IGNORE:
                            deliveredChunk.insertItem(ObjectFactory.buildIgnoredChunkItem(item.current.getId(), "Ignored by diff processor", item.current.getTrackingId()));
                            break;
                        default:
                            throw new SinkException("Unknown chunk item state: " + item.current.getStatus().name());
                    }
                }
            }
        } finally {
            DBCTrackedLogContext.remove();
        }
        return deliveredChunk;
    }

    private ChunkItem compareFailedItems(ChunkItemPair item) {
        // we are only interested in chunk items with a single diagnostic
        // containing a FailRecord message
        if (item.current.getDiagnostics().size() == 1
                && item.next.getDiagnostics().size() == 1) {
            Diagnostic currentDiagnostic = item.current.getDiagnostics().get(0);
            Diagnostic nextDiagnostic = item.next.getDiagnostics().get(0);

            // PMD wants all these checks inside a single if even though readability suffers
            if (currentDiagnostic.getTag() != null
                    && currentDiagnostic.getTag().equals(FailRecord.class.getName())
                    && nextDiagnostic.getTag() != null
                    && nextDiagnostic.getTag().equals(FailRecord.class.getName())
                    && currentDiagnostic.getMessage().equals(nextDiagnostic.getMessage())) {
                return ObjectFactory.buildSuccessfulChunkItem(item.current.getId(),
                    "Current and Next output were identical", ChunkItem.Type.STRING,
                    item.current.getTrackingId());
            }
        }
        return ObjectFactory.buildIgnoredChunkItem(item.current.getId(),
            "Failed by diff processor", item.current.getTrackingId());
    }

    /*
     * Private methods
     */

    private Chunk failWithMissingNextItem(Chunk processedChunk) {
        final Chunk deliveredChunk = new Chunk(processedChunk.getJobId(), processedChunk.getChunkId(), Chunk.Type.DELIVERED);

        for (final ChunkItem item : processedChunk) {
            ChunkItem chunkItem = ObjectFactory.buildFailedChunkItem(item.getId(), "Missing Next Items", ChunkItem.Type.STRING, item.getTrackingId());
            chunkItem.appendDiagnostics(ObjectFactory.buildFatalDiagnostic("Missing Next Items"));
            deliveredChunk.insertItem(chunkItem);
        }
        return deliveredChunk;

    }

    /*
     * This method creates a ChunkItem containing the diff result.
     * If the diff result is an empty String, the item is converted into a ChunkItem with status SUCCESS
     * If the diff result is NOT an empty String, the item is converted into a ChunkItem with status FAILURE
     */
    private ChunkItem getChunkItemWithDiffResult(ChunkItemPair item) {
        String diff;
        ChunkItem chunkItem;
        try {
            try {
                diff = addiDiffGenerator.getDiff(getAddiRecord(item.current.getData()), getAddiRecord(item.next.getData()));
            } catch (IllegalArgumentException e) {
                diff = getXmlDiff(item.current.getData(), item.next.getData());
            }
            if (diff.isEmpty()) {
                chunkItem = ObjectFactory.buildSuccessfulChunkItem(item.current.getId(), "Current and Next output were identical", ChunkItem.Type.STRING, item.current.getTrackingId());
            } else {
                chunkItem = ObjectFactory.buildFailedChunkItem(item.current.getId(), diff, ChunkItem.Type.STRING, item.current.getTrackingId());
                chunkItem.appendDiagnostics(ObjectFactory.buildFatalDiagnostic("Diff created: Current and Next output were not identical"));
            }
        } catch (DiffGeneratorException e) {
            chunkItem = ObjectFactory.buildFailedChunkItem(item.current.getId(), StringUtil.getStackTraceString(e, ""), ChunkItem.Type.STRING, item.current.getTrackingId());
            chunkItem.appendDiagnostics(ObjectFactory.buildFatalDiagnostic("Exception occurred while comparing addi records", e));
        }
        return chunkItem;
    }

    private AddiRecord getAddiRecord(byte[] data) {
        final AddiReader currentAddiReader = new AddiReader(new ByteArrayInputStream(data));
        try {
            return currentAddiReader.getNextRecord();
        } catch (Exception e) {
            throw new IllegalArgumentException("input byte array cannot be converted to addi", e);
        }
    }

    private String getXmlDiff(byte[]currentData, byte[] nextData) throws DiffGeneratorException {
        return externalToolDiffGenerator.getDiff(ExternalToolDiffGenerator.Kind.XML,
                currentData, nextData);
    }


    static private String statusToString( ChunkItem.Status status) {
        switch ( status ) {
            case FAILURE: return "Failure";
            case SUCCESS: return "Success";
            case IGNORE: return "Ignore";
            default:
                return "Internal Error: Unknown Status";
        }
    }

    static class ChunkItemPair {
        public ChunkItemPair(ChunkItem current, ChunkItem next) {
            this.current = current;
            this.next = next;
        }

        public ChunkItem current;
        public ChunkItem next;
    }

    private List<ChunkItemPair> buildCurrentNextChunkList(Chunk processed) {
        final List<ChunkItem> items = processed.getItems();
        final List<ChunkItem> next = processed.getNext();
        if( items.size() != next.size() ) {
            throw new IllegalArgumentException("Internal Error item and next length differ");
        }
        final List<ChunkItemPair> result = new ArrayList<>();
        for( int i = 0 ; i < items.size() ; i++ ) {
            result.add(new ChunkItemPair(items.get(i), next.get(i)));
        }
        return result;
    }
}
