package dk.dbc.dataio.sequenceanalyser.naive;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.sequenceanalyser.SequenceAnalyser;

import java.util.List;

public class NaiveSequenceAnalyser implements SequenceAnalyser {

    private NaiveDependencyGraph dependencyGraph = new NaiveDependencyGraph();

    @Override
    public void addChunk(Chunk chunk) {
        dependencyGraph.insert(chunk);
    }

    @Override
    public void deleteAndReleaseChunk(ChunkIdentifier identifier) {
        dependencyGraph.deleteAndRelease(identifier);
    }

    @Override
    public List<ChunkIdentifier> getInactiveIndependentChunks() {
        return dependencyGraph.getInactiveIndependentChunksAndActivate();
    }

    // Number of elements in internal data-structure.
    @Override
    public int size() {
        return dependencyGraph.size();
    }

    @Override
    public boolean isHead(ChunkIdentifier chunkIdentifier) {
        return dependencyGraph.isHead(chunkIdentifier);
    }

}
