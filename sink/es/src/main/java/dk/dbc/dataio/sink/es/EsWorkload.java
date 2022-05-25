package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.EsSinkConfig;
import dk.dbc.dataio.commons.types.ObjectFactory;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.dataio.sink.es.entity.es.TaskSpecificUpdateEntity;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.dataio.sink.util.AddiUtil;
import dk.dbc.log.DBCTrackedLogContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EsWorkload {
    final Chunk deliveredChunk;
    final List<AddiRecord> addiRecords;
    final int userId;
    final TaskSpecificUpdateEntity.UpdateAction action;

    public EsWorkload(Chunk deliveredChunk, List<AddiRecord> addiRecords,
                      int userId, TaskSpecificUpdateEntity.UpdateAction action) {
        this.deliveredChunk = InvariantUtil.checkNotNullOrThrow(deliveredChunk, "deliveredChunk");
        this.addiRecords = InvariantUtil.checkNotNullOrThrow(addiRecords, "addiRecords");
        this.userId = userId;
        this.action = InvariantUtil.checkNotNullOrThrow(action, "action");
    }

    public List<AddiRecord> getAddiRecords() {
        return addiRecords;
    }

    public Chunk getDeliveredChunk() {
        return deliveredChunk;
    }

    public int getUserId() {
        return userId;
    }

    public TaskSpecificUpdateEntity.UpdateAction getAction() {
        return action;
    }


    public static EsWorkload create(Chunk processedChunk, EsSinkConfig sinkConfig, AddiRecordPreprocessor addiRecordPreprocessor) throws SinkException {
        InvariantUtil.checkNotNullOrThrow(processedChunk, "processedChunk");
        InvariantUtil.checkNotNullOrThrow(sinkConfig, "sinkConfig");
        InvariantUtil.checkNotNullOrThrow(addiRecordPreprocessor, "addiRecordPreprocessor");

        final int numberOfItems = processedChunk.size();
        final List<AddiRecord> addiRecords = new ArrayList<>(numberOfItems);
        final Chunk incompleteDeliveredChunk = new Chunk(processedChunk.getJobId(), processedChunk.getChunkId(), Chunk.Type.DELIVERED);
        incompleteDeliveredChunk.setEncoding(processedChunk.getEncoding());

        try {
            for (ChunkItem chunkItem : processedChunk) {
                final String trackingId = chunkItem.getTrackingId();
                DBCTrackedLogContext.setTrackingId(trackingId);
                switch (chunkItem.getStatus()) {
                    case SUCCESS:
                        try {
                            final List<AddiRecord> addiRecordsFromItem = getAddiRecords(chunkItem, addiRecordPreprocessor);
                            addiRecords.addAll(addiRecordsFromItem);
                            // We use the data property of the ChunkItem placeholder kept in the ES
                            // in-flight database to store the number of Addi records from the
                            // original record - this information is used by the EsCleanupBean
                            // when creating the resulting sink chunk.
                            incompleteDeliveredChunk.insertItem(ObjectFactory.buildSuccessfulChunkItem(
                                    chunkItem.getId(), Integer.toString(addiRecordsFromItem.size()), ChunkItem.Type.UNKNOWN, trackingId));
                        } catch (RuntimeException | IOException e) {
                            ChunkItem processedItem = ObjectFactory.buildFailedChunkItem(chunkItem.getId(),
                                    String.format("Exception caught while retrieving addi records: %s", e.toString()),
                                    ChunkItem.Type.STRING, trackingId);
                            processedItem.appendDiagnostics(ObjectFactory.buildFatalDiagnostic("Exception caught while retrieving addi records", e));
                            incompleteDeliveredChunk.insertItem(processedItem);
                        }
                        break;
                    case FAILURE:
                        incompleteDeliveredChunk.insertItem(ObjectFactory.buildIgnoredChunkItem(chunkItem.getId(), "Failed by processor", trackingId));
                        break;
                    case IGNORE:
                        incompleteDeliveredChunk.insertItem(ObjectFactory.buildIgnoredChunkItem(chunkItem.getId(), "Ignored by processor", trackingId));
                        break;
                    default:
                        throw new SinkException("Unknown chunk item state: " + chunkItem.getStatus().name());
                }
            }
        } finally {
            DBCTrackedLogContext.remove();
        }
        return new EsWorkload(incompleteDeliveredChunk, addiRecords, sinkConfig.getUserId(), TaskSpecificUpdateEntity.UpdateAction.valueOf(sinkConfig.getEsAction()));
    }

    private static List<AddiRecord> getAddiRecords(ChunkItem chunkItem, AddiRecordPreprocessor addiRecordPreprocessor) throws IllegalArgumentException, IOException, SinkException {
        final List<AddiRecord> addiRecords = AddiUtil.getAddiRecordsFromChunkItem(chunkItem);
        final List<AddiRecord> preprocessedAddiRecords = new ArrayList<>(addiRecords.size());
        preprocessedAddiRecords.addAll(addiRecords.stream().map(addiRecord -> addiRecordPreprocessor.execute(addiRecord, chunkItem.getTrackingId())).collect(Collectors.toList()));
        return preprocessedAddiRecords;
    }
}
