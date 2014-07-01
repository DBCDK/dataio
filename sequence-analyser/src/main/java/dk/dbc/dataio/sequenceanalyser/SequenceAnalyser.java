package dk.dbc.dataio.sequenceanalyser;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.sequenceanalyser.naive.ChunkIdentifier;
import dk.dbc.dataio.sequenceanalyser.naive.NaiveSequenceAnalyser;
import java.util.List;

public interface SequenceAnalyser {

    public void addChunk(Chunk chunk, Sink sink);

    // throws if not active?
    public void deleteAndReleaseChunk(ChunkIdentifier identifier);

    public List<ChunkIdentifier> getInactiveIndependentChunks();

    // ignore if already active or nonexisting
    public void activateChunk(ChunkIdentifier identifier);

    // Number of elements in internal data-structure.
    public int size();
}
