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
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.sink.types.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.javascript.recordprocessing.FailRecord;
import dk.dbc.log.DBCTrackedLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

@MessageDriven
public class DiffMessageProcessorBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiffMessageProcessorBean.class);

    @EJB AddiDiffGenerator addiDiffGenerator;
    @EJB ExternalToolDiffGenerator externalToolDiffGenerator;

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage)
            throws ServiceException, InvalidMessageException {
        final Chunk chunk = unmarshallPayload(consumedMessage);
        LOGGER.info("Received chunk {}/{}", chunk.getJobId(), chunk.getChunkId());
        final Chunk result = handleChunk(chunk);
        uploadChunk(result);
    }

    /**
     * <br/> All 'current' input items have their status compared with the status of their 'next' counterpart.
     * <br/> If status differs, a item is created with status FAILURE in the result chunk.
     * <br/> If status of 'current' and 'next is identical:
     * <br/> All input items with status IGNORE are converted into IGNORE items in result.
     * <br/> All input items with status FAILURE are converted into IGNORE items in result.
     * <br/> All 'current' input items with status SUCCESS have their data compared with the data of their 'next' counterpart:
     * <br/>  - If the diff produces an empty string, the item is converted into a SUCCESS item in result.
     * <br/>  - If the diff produces a non-empty string, the item is converted into a FAILURE item in result.
     * <br/>  - If a DiffGeneratorException is thrown while comparing, the item is converted into a FAILURE item result.
     * @param chunk processed chunk
     * @return result of diff
     * @throws SinkException on failure to produce diff
     */
    Chunk handleChunk(Chunk chunk) throws SinkException {
        if(!chunk.hasNextItems()) {
            return failWithMissingNextItem(chunk);
        }

        final Chunk result = new Chunk(chunk.getJobId(), chunk.getChunkId(), Chunk.Type.DELIVERED);
        try {
            for (final ChunkItemPair item : buildCurrentNextChunkList(chunk)) {
                DBCTrackedLogContext.setTrackingId(item.current.getTrackingId());
                LOGGER.info("Handling item {}/{}/{}",
                        chunk.getJobId(), chunk.getChunkId(), item.current.getId());
                if (item.current.getStatus() != item.next.getStatus()) {
                    final String message = String.format("Different status %s -> %s\n%s",
                            statusToString(item.current.getStatus()),
                            statusToString(item.next.getStatus()),
                            StringUtil.asString(item.next.getData())
                    );
                    result.insertItem(ChunkItem.failedChunkItem()
                            .withId(item.current.getId())
                            .withData(message)
                            .withType(ChunkItem.Type.STRING)
                            .withTrackingId(item.current.getTrackingId())
                            .withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL, message)));
                    continue;
                }

                switch (item.current.getStatus()) {
                    case SUCCESS:
                        result.insertItem(getChunkItemWithDiffResult(item));
                        break;
                    case FAILURE:
                        result.insertItem(compareFailedItems(item));
                        break;
                    case IGNORE:
                        result.insertItem(ChunkItem.ignoredChunkItem()
                                .withId(item.current.getId())
                                .withData("Ignored by diff sink")
                                .withTrackingId(item.current.getTrackingId()));
                        break;
                    default:
                        throw new SinkException("Unknown chunk item state: " +
                                item.current.getStatus().name());
                }
            }
        } finally {
            DBCTrackedLogContext.remove();
        }
        return result;
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
