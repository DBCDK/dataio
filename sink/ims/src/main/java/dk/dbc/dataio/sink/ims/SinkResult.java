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

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ObjectFactory;
import dk.dbc.oss.ns.updatemarcxchange.MarcXchangeRecord;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeResult;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeStatusEnum;

import javax.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SinkResult {

    private final ChunkItem[] chunkItems;
    private final List<MarcXchangeRecord> marcXchangeRecords;
    private final long jobId;
    private final long chunkId;

    public SinkResult(Chunk chunk, MarcXchangeRecordUnmarshaller marcXchangeRecordUnmarshaller) {
        chunkItems = new ChunkItem[chunk.size()];
        marcXchangeRecords = new ArrayList<>();
        jobId = chunk.getJobId();
        chunkId = chunk.getChunkId();

        for (ChunkItem chunkItem : chunk.getItems()) {
            switch (chunkItem.getStatus()) {
                case SUCCESS:
                    try {
                        marcXchangeRecords.add(marcXchangeRecordUnmarshaller.toMarcXchangeRecord(chunkItem));
                    } catch (JAXBException e) {
                        final String message = "Error occurred while unmarshalling JAXBElement";
                        final ChunkItem failedChunkItem = ObjectFactory.buildFailedChunkItem(
                                chunkItem.getId(), message, ChunkItem.Type.STRING);
                        failedChunkItem.appendDiagnostics(ObjectFactory.buildFatalDiagnostic(message, e));
                        chunkItems[((int) chunkItem.getId())] = failedChunkItem;
                    }
                    break;

                case FAILURE:
                    chunkItems[((int) chunkItem.getId())] = ObjectFactory.buildIgnoredChunkItem(
                            chunkItem.getId(), "Failed by processor", chunkItem.getTrackingId());
                    break;

                case IGNORE:
                    chunkItems[((int) chunkItem.getId())] = ObjectFactory.buildIgnoredChunkItem(
                            chunkItem.getId(), "Ignored by processor", chunkItem.getTrackingId());
                    break;
            }
        }
    }

    /**
     * Updates the internal list of chunk items depending on the status of the UpdateMarcXchangeResults
     * @param updateMarcXchangeResults list containing the results for each updated record
     */
    public void update(List<UpdateMarcXchangeResult> updateMarcXchangeResults) {
        for(UpdateMarcXchangeResult updateMarcXchangeResult : updateMarcXchangeResults) {
            final String itemData = buildItemData(updateMarcXchangeResult);
            if(updateMarcXchangeResult.getUpdateMarcXchangeStatus() == UpdateMarcXchangeStatusEnum.OK) {
                chunkItems[Integer.parseInt(updateMarcXchangeResult.getMarcXchangeRecordId())] =
                        ObjectFactory.buildSuccessfulChunkItem(Long.valueOf(updateMarcXchangeResult.getMarcXchangeRecordId()), itemData, ChunkItem.Type.STRING);
            } else {
                chunkItems[Integer.parseInt(updateMarcXchangeResult.getMarcXchangeRecordId())] =
                        ObjectFactory.buildFailedChunkItem(Long.valueOf(updateMarcXchangeResult.getMarcXchangeRecordId()), itemData, ChunkItem.Type.STRING);
            }
        }
    }

    public List<MarcXchangeRecord> getMarcXchangeRecords() {
        return marcXchangeRecords;
    }

    public Chunk toChunk() {
        Chunk chunk = new Chunk(jobId, chunkId, Chunk.Type.DELIVERED);
        chunk.addAllItems(new ArrayList<>(Arrays.asList(chunkItems)));
        return chunk;
    }

    /*
     * private methods
     */

    private String buildItemData(UpdateMarcXchangeResult updateMarcXchangeResult) {
        final StringBuilder stringBuilder = new StringBuilder("Status: ").append(updateMarcXchangeResult.getUpdateMarcXchangeStatus().value());
        final String message = updateMarcXchangeResult.getUpdateMarcXchangeMessage();
        if(message != null && !message.isEmpty()){
            stringBuilder.append(", Message: ").append(message);
        }
        return stringBuilder.toString();
    }
}