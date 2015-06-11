package dk.dbc.dataio.sequenceanalyser;

import java.util.Set;

public class CollisionDetectionElement {
    private final CollisionDetectionElementIdentifier identifier;
    private final Set<String> keys;
    private final int slotsConsumed;

    public CollisionDetectionElement(CollisionDetectionElementIdentifier identifier, Set<String> keys, int slotsConsumed) {
        this.identifier = identifier;
        this.keys = keys;
        this.slotsConsumed = slotsConsumed;
    }

    public CollisionDetectionElementIdentifier getIdentifier() {
        return identifier;
    }

    public Set<String> getKeys() {
        return keys;
    }

    public int getSlotsConsumed() {
        return slotsConsumed;
    }
}
