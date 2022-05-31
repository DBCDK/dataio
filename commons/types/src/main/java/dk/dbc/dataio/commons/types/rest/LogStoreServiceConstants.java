package dk.dbc.dataio.commons.types.rest;

public class LogStoreServiceConstants {
    public static final String JOB_ID_VARIABLE = "jobId";
    public static final String CHUNK_ID_VARIABLE = "chunkId";
    public static final String ITEM_ID_VARIABLE = "itemId";

    public static final String ITEM_LOG_ENTRY_COLLECTION = "logentries/jobs/{jobId}/chunks/{chunkId}/items/{itemId}";
    public static final String JOB_LOG_ENTRY_COLLECTION = "logentries/jobs/{jobId}";

    private LogStoreServiceConstants() {
    }
}
