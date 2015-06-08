package dk.dbc.dataio.sequenceanalyser.naive;

import dk.dbc.dataio.sequenceanalyser.CollisionDetectionElement;
import dk.dbc.dataio.sequenceanalyser.CollisionDetectionElementIdentifier;
import dk.dbc.dataio.sequenceanalyser.SequenceAnalyser;

import java.util.List;

public class NaiveSequenceAnalyser implements SequenceAnalyser {

    private NaiveDependencyGraph dependencyGraph = new NaiveDependencyGraph();

    @Override
    public void add(CollisionDetectionElement element) {
        dependencyGraph.insert(element);
    }

    @Override
    public int deleteAndRelease(CollisionDetectionElementIdentifier identifier) {
        return dependencyGraph.deleteAndRelease(identifier);
    }

    @Override
    public List<CollisionDetectionElement> getInactiveIndependent(int maxSlotsSoftLimit) {
        return dependencyGraph.getInactiveIndependentChunksAndActivate(maxSlotsSoftLimit);
    }

    // Number of elements in internal data-structure.
    @Override
    public int size() {
        return dependencyGraph.size();
    }

    @Override
    public boolean isHead(CollisionDetectionElementIdentifier identifier) {
        return dependencyGraph.isHead(identifier);
    }

}
