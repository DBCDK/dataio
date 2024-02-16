package dk.dbc.dataio.jobstore.distributed;

public enum QueueSubmitMode {
    DIRECT,              // enqueue chunk directly
    BULK,                // mark chunk ready for processing/delivery
    TRANSITION_TO_DIRECT // transition from BULK to DIRECT mode
}
