package dk.dbc.dataio.commons.types;

public class ItemCompletionState {

    public enum State {
        SUCCESS,
        FAILURE,
        IGNORED,
        INCOMPLETE
    }

    private /* final */ long itemId;
    private /* final */ State state;

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
