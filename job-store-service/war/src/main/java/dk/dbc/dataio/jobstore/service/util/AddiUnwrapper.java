package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.invariant.InvariantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Addi format unwrapper
 */
public class AddiUnwrapper implements ChunkItemUnwrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddiUnwrapper.class);

    /**
     * Unwraps content from Addi records contained in given chunk item.
     * If chunk type is {@link dk.dbc.dataio.commons.types.ChunkItem.Type#UNKNOWN}
     * and content NOT contains an Addi record, the chunk item is
     * added to the result list unchanged.
     *
     * @param wrappedChunkItem chunk item containing Addi records
     * @return list of unwrapped chunk items
     * @throws NullPointerException if given null valued chunk item
     * @throws JobStoreException    if wrapping type of given chunk item is not
     *                              {@link dk.dbc.dataio.commons.types.ChunkItem.Type#ADDI} or
     *                              {@link dk.dbc.dataio.commons.types.ChunkItem.Type#UNKNOWN}
     */
    @Override
    public List<ChunkItem> unwrap(ChunkItem wrappedChunkItem) throws NullPointerException, JobStoreException {
        return addiUnwrap(InvariantUtil.checkNotNullOrThrow(wrappedChunkItem, "wrappedChunkItem"));
    }

    private List<ChunkItem.Type> getUnwrappedType(ChunkItem wrappedChunkItem) throws JobStoreException {
        final List<ChunkItem.Type> type = wrappedChunkItem.getType();
        final List<ChunkItem.Type> unwrappedType = new ArrayList<>(Math.max(type.size() - 1, 1));
        if (type.size() == 1) {
            if (type.get(0) == ChunkItem.Type.UNKNOWN) {
                // Special case handling of chunk items since
                // the type system is not fully implemented.
                unwrappedType.add(ChunkItem.Type.UNKNOWN);
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
        final List<ChunkItem.Type> unwrappedType = getUnwrappedType(wrappedChunkItem);
        final ArrayList<ChunkItem> unwrappedChunkItems = new ArrayList<>();
        try {
            final List<Diagnostic> diagnostics = wrappedChunkItem.getDiagnostics();
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

                unwrappedChunkItem.appendDiagnostics(diagnostics);
                unwrappedChunkItems.add(unwrappedChunkItem);

                record = addiReader.getNextRecord();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to parse chunk item as Addi", e);
            unwrappedChunkItems.add(wrappedChunkItem);
        }
        return unwrappedChunkItems;
    }
}
