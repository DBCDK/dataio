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
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.service.AbstractSinkMessageConsumerBean;
import dk.dbc.dataio.jobstore.types.JobError;
import dk.dbc.dataio.sink.types.SinkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

@MessageDriven
public class DiffMessageProcessorBean extends AbstractSinkMessageConsumerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiffMessageProcessorBean.class);

    @EJB
    JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    @Override
    public void handleConsumedMessage(ConsumedMessage consumedMessage) throws ServiceException, InvalidMessageException {
        final ExternalChunk processedChunk = unmarshallPayload(consumedMessage);
        final ExternalChunk deliveredChunk = processPayload(processedChunk);
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


    private ExternalChunk failWithMissingNextItem(ExternalChunk processedChunk) {
        final ExternalChunk deliveredChunk = new ExternalChunk(processedChunk.getJobId(), processedChunk.getChunkId(), ExternalChunk.Type.DELIVERED);

        for (final ChunkItem item : processedChunk) {
            deliveredChunk.insertItem(new ChunkItem(item.getId(), StringUtil.asBytes("Missing Next Items"), ChunkItem.Status.FAILURE));
        }
        return deliveredChunk;

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
    ExternalChunk processPayload(ExternalChunk processedChunk) throws SinkException{
        if( !processedChunk.hasNextItems()) {
            return failWithMissingNextItem( processedChunk );
        }

        final ExternalChunk deliveredChunk = new ExternalChunk(processedChunk.getJobId(), processedChunk.getChunkId(), ExternalChunk.Type.DELIVERED);
        for (final ChunkItemPair item : buildCurrentNextChunkList(processedChunk)) {
            if( item.current.getStatus() != item.next.getStatus() ) {
                String message = String.format("Different status %s -> %s\n%s",
                        statusToString(item.current.getStatus()),
                        statusToString(item.next.getStatus()),
                        StringUtil.asString(item.next.getData())
                );
                deliveredChunk.insertItem(new ChunkItem(item.next.getId(), StringUtil.asBytes(message), ChunkItem.Status.FAILURE));
            } else {
                switch (item.current.getStatus()) {
                    case SUCCESS:
                        deliveredChunk.insertItem(getChunkItemWithDiffResult(
                                item, processedChunk.getEncoding()));
                        break;
                    case FAILURE:
                        deliveredChunk.insertItem(new ChunkItem(
                                item.current.getId(), StringUtil.asBytes("Failed by diff processor"), ChunkItem.Status.IGNORE));
                        break;
                    case IGNORE:
                        deliveredChunk.insertItem(new ChunkItem(
                                item.current.getId(), StringUtil.asBytes("Ignored by diff processor"), ChunkItem.Status.IGNORE));
                        break;
                    default:
                        throw new SinkException("Unknown chunk item state: " + item.current.getStatus().name());
                }
            }
        }
        return deliveredChunk;
    }

    /*
     * This method creates a ChunkItem containing the diff result.
     * If the diff result is an empty String, the item is converted into a ChunkItem with status SUCCESS
     * If the diff result is NOT an empty String, the item is converted into a ChunkItem with status FAILURE
     */
    private ChunkItem getChunkItemWithDiffResult(ChunkItemPair item, Charset encoding) {
        String diff;
        ChunkItem chunkItem;
        try {
            try {
                final AddiDiffGenerator addiDiffGenerator = new AddiDiffGenerator();
                diff = addiDiffGenerator.getDiff(getAddiRecord(item.current.getData()), getAddiRecord(item.next.getData()));
            } catch (IllegalArgumentException e) {
                diff = getXmlDiff(item.current.getData(), item.next.getData());
            }
            if (diff.isEmpty()) {
                chunkItem = new ChunkItem(item.current.getId(), StringUtil.asBytes("Current and Next output were identical", encoding), ChunkItem.Status.SUCCESS);
            } else {
                chunkItem = new ChunkItem(item.current.getId(), StringUtil.asBytes(diff, encoding), ChunkItem.Status.FAILURE);
            }
        } catch (DiffGeneratorException e) {
            chunkItem = new ChunkItem(item.current.getId(), StringUtil.asBytes(StringUtil.getStackTraceString(e, "")), ChunkItem.Status.FAILURE);
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
        final XmlDiffGenerator xmlDiffGenerator = new XmlDiffGenerator();
        return xmlDiffGenerator.getDiff(currentData, nextData);
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

    List<ChunkItemPair> buildCurrentNextChunkList( ExternalChunk processed ) {
        final List<ChunkItem> items=processed.getItems();
        final List<ChunkItem> next=processed.getNext();
        if( items.size() != next.size() ) {
            throw new IllegalArgumentException("Internal Error item and next length differ");
        }
        final List<ChunkItemPair> result=new ArrayList<>();
        for( int i = 0 ; i < items.size() ; i++ ) {
            result.add( new ChunkItemPair(items.get(i), next.get(i)));
        }
        return result;
    }
}