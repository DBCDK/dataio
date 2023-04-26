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

    final ChunkItem[] chunkItems; //package scoped due to unit tests
    private final List<MarcXchangeRecord> marcXchangeRecords;
    private final int jobId;
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
                        final ChunkItem failedChunkItem = ChunkItem.failedChunkItem().withId(chunkItem.getId())
                                .withData(message).withTrackingId(chunkItem.getTrackingId()).withType(ChunkItem.Type.STRING)
                                .withDiagnostics(ObjectFactory.buildFatalDiagnostic(message, e));
                        chunkItems[(int) chunkItem.getId()] = failedChunkItem;
                    }
                    break;

                case FAILURE:
                    chunkItems[(int) chunkItem.getId()] = ChunkItem.ignoredChunkItem().withId(chunkItem.getId())
                            .withData("Failed by processor").withType(ChunkItem.Type.STRING).withTrackingId(chunkItem.getTrackingId());
                    break;

                case IGNORE:
                    chunkItems[(int) chunkItem.getId()] = ChunkItem.ignoredChunkItem().withId(chunkItem.getId())
                            .withData("Ignored by processor").withType(ChunkItem.Type.STRING).withTrackingId(chunkItem.getTrackingId());
                    break;
            }
        }
    }

    /**
     * Updates the internal list of chunk items depending on the status of the UpdateMarcXchangeResults
     *
     * @param updateMarcXchangeResults list containing the results for each updated record
     */
    public void update(List<UpdateMarcXchangeResult> updateMarcXchangeResults) {
        if (!(hasFailedServiceValidation(updateMarcXchangeResults)
                || hasTooFewResults(updateMarcXchangeResults))) {
            insertChunkItems(updateMarcXchangeResults);
        }
    }

    public List<MarcXchangeRecord> getMarcXchangeRecords() {
        return marcXchangeRecords;
    }

    public Chunk toChunk() {
        Chunk chunk = new Chunk(jobId, chunkId, Chunk.Type.DELIVERED);
        chunk.addAllItems(Arrays.asList(chunkItems));
        return chunk;
    }

    private boolean hasFailedServiceValidation(List<UpdateMarcXchangeResult> updateMarcXchangeResults) {
        if (updateMarcXchangeResults.size() == 1) {
            final UpdateMarcXchangeResult updateMarcXchangeResult = updateMarcXchangeResults.get(0);
            if (updateMarcXchangeResult.getUpdateMarcXchangeStatus() != UpdateMarcXchangeStatusEnum.OK && updateMarcXchangeResult.getMarcXchangeRecordId() == null) {
                insertFailedChunkItems(updateMarcXchangeResults.get(0),
                        "Item failed due to webservice returning updateMarcXchangeResult with record id null.");
                return true;
            }
        }
        return false;
    }

    private boolean hasTooFewResults(List<UpdateMarcXchangeResult> updateMarcXchangeResults) {
        if (updateMarcXchangeResults.size() != marcXchangeRecords.size()) {
            insertFailedChunkItems(null, String.format("Item failed due to webservice returning %s updateMarcXchangeResults when %d was expected.",
                    updateMarcXchangeResults.size(), marcXchangeRecords.size()));
            return true;
        }
        return false;
    }

    /*
     * replaces null values in the list of chunk items with failed chunk items
     */
    private void insertFailedChunkItems(UpdateMarcXchangeResult updateMarcXchangeResult, String message) {
        final String itemData = buildItemData(updateMarcXchangeResult, message);
        for (int i = 0; i < chunkItems.length; i++) {
            if (chunkItems[i] == null) {
                chunkItems[i] = ChunkItem.failedChunkItem().withId(i).withData(itemData).withType(ChunkItem.Type.STRING)
                        .withDiagnostics(ObjectFactory.buildFatalDiagnostic(itemData));
            }
        }
    }

    /*
     * replaces null values in the list of chunk items with chunk items.
     * The status of each chunk item inserted is dictated by the input
     */
    private void insertChunkItems(List<UpdateMarcXchangeResult> updateMarcXchangeResults) {
        for (UpdateMarcXchangeResult updateMarcXchangeResult : updateMarcXchangeResults) {
            final String itemData = buildItemData(updateMarcXchangeResult, null);
            if (updateMarcXchangeResult.getUpdateMarcXchangeStatus() == UpdateMarcXchangeStatusEnum.OK) {
                chunkItems[Integer.parseInt(updateMarcXchangeResult.getMarcXchangeRecordId())] =
                        ChunkItem.successfulChunkItem().withId(Long.parseLong(updateMarcXchangeResult.getMarcXchangeRecordId()))
                                .withData(itemData).withType(ChunkItem.Type.STRING);
            } else {
                chunkItems[Integer.parseInt(updateMarcXchangeResult.getMarcXchangeRecordId())] =
                        ChunkItem.failedChunkItem().withId(Long.parseLong(updateMarcXchangeResult.getMarcXchangeRecordId()))
                                .withData(itemData).withType(ChunkItem.Type.STRING).withDiagnostics(ObjectFactory.buildFatalDiagnostic(itemData));
            }
        }
    }

    private String buildItemData(UpdateMarcXchangeResult updateMarcXchangeResult, String errorMessage) {
        if (updateMarcXchangeResult == null) {
            return errorMessage == null ? "" : errorMessage;
        } else {
            final StringBuilder stringBuilder = new StringBuilder();
            if (errorMessage != null) {
                stringBuilder.append(errorMessage).append(" -> ");
            }
            stringBuilder.append("[Status: ").append(updateMarcXchangeResult.getUpdateMarcXchangeStatus().value()).append("]");
            final String updateMarcXchangeMessage = updateMarcXchangeResult.getUpdateMarcXchangeMessage();
            if (updateMarcXchangeMessage != null && !updateMarcXchangeMessage.isEmpty()) {
                stringBuilder.append(", [Message: ").append(updateMarcXchangeMessage).append("]");
            }
            return stringBuilder.toString();
        }
    }
}
