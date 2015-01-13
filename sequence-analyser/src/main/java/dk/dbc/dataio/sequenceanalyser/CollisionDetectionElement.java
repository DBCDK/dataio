package dk.dbc.dataio.sequenceanalyser;

import java.util.Set;

public class CollisionDetectionElement {

    private final ChunkIdentifier identifier;
    private final Set<String> keys;

    public CollisionDetectionElement(ChunkIdentifier identifier, Set<String> keys) {
        this.identifier = identifier;
        this.keys = keys;
    }

    public ChunkIdentifier getIdentifier() {
        return identifier;
    }

    public Set<String> getKeys() {
        return keys;
    }
}
