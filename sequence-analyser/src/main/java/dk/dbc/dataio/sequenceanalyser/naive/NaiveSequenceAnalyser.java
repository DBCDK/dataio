package dk.dbc.dataio.sequenceanalyser.naive;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.sequenceanalyser.SequenceAnalyser;
import java.util.List;

public class NaiveSequenceAnalyser implements SequenceAnalyser {

    private NaiveDependencyGraph dependencyGraph = new NaiveDependencyGraph();

    @Override
    public void addChunk(Chunk chunk, Sink sink) {
        dependencyGraph.insertChunkIntoDependencyGraph(chunk, sink);
    }

    // throws if not active?
    @Override
    public void deleteAndReleaseChunk(ChunkIdentifier identifier) {
        dependencyGraph.deleteAndReleaseChunk(identifier);
    }

    @Override
    public List<ChunkIdentifier> getInactiveIndependentChunks() {
        return dependencyGraph.getIndependentChunks();
    }

    // ignore if already active or nonexisting
    @Override
    public void activateChunk(ChunkIdentifier identifier) {

    }

    // Number of elements in internal data-structure.
    @Override
    public int size() {
        return 0;
    }

}
