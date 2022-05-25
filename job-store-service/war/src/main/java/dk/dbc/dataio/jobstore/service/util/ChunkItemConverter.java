package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.jobstore.types.JobStoreException;

import java.nio.charset.Charset;
import java.util.List;

@FunctionalInterface
public interface ChunkItemConverter {
    byte[] convert(ChunkItem chunkItem, Charset encodedAs, List<Diagnostic> diagnostics) throws JobStoreException;
}
