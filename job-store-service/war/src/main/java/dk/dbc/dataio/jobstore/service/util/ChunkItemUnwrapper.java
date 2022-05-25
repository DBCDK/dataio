package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.jobstore.types.JobStoreException;

import java.util.List;

/**
 * General interface to unwrap content from wrapping formats
 */
@FunctionalInterface
public interface ChunkItemUnwrapper {
    /**
     * Unwraps content from given chunk item
     * @param chunkItem chunk item to unwrap
     * @return list (since some wrapping formats may wrap multiple items) of unwrapped chunk items
     * @throws JobStoreException on failure to unwrap
     */
    List<ChunkItem> unwrap(ChunkItem chunkItem) throws JobStoreException;
}
