package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.types.JobStoreException;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Addi format unwrapper
 */
public class AddiUnwrapper implements ChunkItemUnwrapper {
    /**
     * Unwraps content from Addi records contained in given chunk item
     * @param wrappedChunkItem chunk item containing Addi records
     * @return list of unwrapped chunk items
     * @throws NullPointerException if given null valued chunk item
     * @throws JobStoreException if wrapping type of given chunk item is not
     * {@link dk.dbc.dataio.commons.types.ChunkItem.Type#ADDI} or if chunk item contains invalid Addi data
     */
    @Override
    public List<ChunkItem> unwrap(ChunkItem wrappedChunkItem) throws NullPointerException, JobStoreException {
        return addiUnwrap(InvariantUtil.checkNotNullOrThrow(wrappedChunkItem, "wrappedChunkItem"));
    }

    private ArrayList<ChunkItem.Type> getUnwrappedType(ChunkItem wrappedChunkItem) throws JobStoreException {
        final ArrayList<ChunkItem.Type> type = wrappedChunkItem.getType();
        final ArrayList<ChunkItem.Type> unwrappedType = new ArrayList<>(Math.max(type.size() - 1, 1));
        if (type.size() == 1) {
            if (type.get(0) == ChunkItem.Type.UNKNOWN) {
                // Special case handling of chunk items from
                // before the type system was implemented.
                // MarcXchange wrapped in Addi is assumed.
                unwrappedType.add(ChunkItem.Type.MARCXCHANGE);
            } else {
                throw new JobStoreException("Type is not wrapped in Addi: " + type.toString());
            }
        } else {
            if (type.get(0) != ChunkItem.Type.ADDI) {
                throw new JobStoreException("Type is not wrapped in Addi: " + type.toString());
            }
            unwrappedType.addAll(type.subList(1, type.size()));
        }
        return unwrappedType;
    }

    private List<ChunkItem> addiUnwrap(ChunkItem wrappedChunkItem) throws JobStoreException {
        final ArrayList<ChunkItem.Type> unwrappedType = getUnwrappedType(wrappedChunkItem);
        try {
            final ArrayList<Diagnostic> diagnostics = wrappedChunkItem.getDiagnostics();
            final ArrayList<ChunkItem> unwrappedChunkItems = new ArrayList<>();
            final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(wrappedChunkItem.getData()));
            AddiRecord record = addiReader.getNextRecord();
            while (record != null) {
                final ChunkItem unwrappedChunkItem = new ChunkItem(
                        wrappedChunkItem.getId(),
                        record.getContentData(),
                        wrappedChunkItem.getStatus(),
                        unwrappedType,
                        wrappedChunkItem.getEncoding()
                );
                if (diagnostics != null) {
                    diagnostics.stream().forEach(unwrappedChunkItem::appendDiagnostics);
                }

                unwrappedChunkItems.add(unwrappedChunkItem);

                record = addiReader.getNextRecord();
            }
            return unwrappedChunkItems;
        } catch(Exception e) {
            throw new JobStoreException("Exception caught while reading Addi content", e);
        }
    }
}
