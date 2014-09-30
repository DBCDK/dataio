package dk.dbc.dataio.commons.types;

import java.io.Serializable;

public class ItemCompletionState implements Serializable {
    private static final long serialVersionUID = -5764793719168906780L;

    public enum State {
        SUCCESS,
        FAILURE,
        IGNORED,
        INCOMPLETE
    }

    private /* final */ long itemId;
    private /* final */ State state;

    private ItemCompletionState() {}

    public ItemCompletionState(long itemId, State state) {
        this.itemId = itemId;
        this.state = state;
    }

    public long getItemId() {
        return itemId;
    }

    public State getState() {
        return state;
    }
}
