package dk.dbc.dataio.sequenceanalyser;

import java.util.Set;

public class CollisionDetectionElement {
    private final CollisionDetectionElementIdentifier identifier;
    private final Set<String> keys;

    public CollisionDetectionElement(CollisionDetectionElementIdentifier identifier, Set<String> keys) {
        this.identifier = identifier;
        this.keys = keys;
    }

    public CollisionDetectionElementIdentifier getIdentifier() {
        return identifier;
    }

    public Set<String> getKeys() {
        return keys;
    }
}
