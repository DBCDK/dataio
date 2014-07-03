package dk.dbc.dataio.sequenceanalyser.naive;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.sequenceanalyser.SequenceAnalyser;
import java.util.List;

public class NaiveSequenceAnalyser implements SequenceAnalyser {

    private NaiveDependencyGraph dependencyGraph = new NaiveDependencyGraph();

    @Override
    public void addChunk(Chunk chunk, Sink sink) {
        dependencyGraph.insert(chunk, sink.getId());
    }

    // throws if not active?
    @Override
    public void deleteAndReleaseChunk(ChunkIdentifier identifier) {
        dependencyGraph.deleteAndRelease(identifier);
    }

    @Override
    public List<ChunkIdentifier> getInactiveIndependentChunks() {
        return dependencyGraph.getInactiveIndependentChunksAndActivate();
    }

    // ignore if already active or nonexisting
    @Override
    public void activateChunk(ChunkIdentifier identifier) {

    }

    // Number of elements in internal data-structure.
    @Override
    public int size() {
        return dependencyGraph.size();
    }

}
