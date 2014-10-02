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
    private /* final */ State chunkifyState;
    private /* final */ State processingState;
    private /* final */ State deliveryState;

    private ItemCompletionState() {}

    @Deprecated
    public ItemCompletionState(long itemId, State state) {
        this.itemId = itemId;
        this.chunkifyState = state;
        this.processingState = state;
        this.deliveryState = state;
    }

    public ItemCompletionState(long itemId, State chunkifyState, State processingState, State deliveryState) {
        this.itemId = itemId;
        this.chunkifyState = chunkifyState;
        this.processingState = processingState;
        this.deliveryState = deliveryState;
    }

    public long getItemId() {
        return itemId;
    }

    public State getChunkifyState() {
        return chunkifyState;
    }
    
    public State getProcessingState() {
        return processingState;
    }
    
    public State getDeliveryState() {
        return deliveryState;
    }
    
    @Deprecated
    public State getState() {
        return processingState;
    }
}
