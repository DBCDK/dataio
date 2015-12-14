package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.jobstore.types.JobStoreException;

import java.nio.charset.Charset;

@FunctionalInterface
public interface ChunkItemConverter {
    byte[] convert(ChunkItem chunkItem, Charset encodedAs) throws JobStoreException;
}
