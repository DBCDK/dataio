package dk.dbc.dataio.commons.types.rest;

public class JobStoreServiceConstants {
    public static final String JOB_ID_VARIABLE = "jobId";
    public static final String CHUNK_ID_VARIABLE = "chunkId";

    public static final String JOB_COLLECTION      = "jobs";
    public static final String JOB_FLOW            = "jobs/{jobId}/flow";
    public static final String JOB_SINK            = "jobs/{jobId}/sink";
    public static final String JOB_STATE           = "jobs/{jobId}/state";
    public static final String JOB_COMPLETIONSTATE = "jobs/{jobId}/completionstate";
    public static final String JOB_CHUNK           = "jobs/{jobId}/chunks/{chunkId}";
    public static final String JOB_PROCESSED       = "jobs/{jobId}/processed/{chunkId}";
    public static final String JOB_DELIVERED       = "jobs/{jobId}/delivered/{chunkId}";

    private JobStoreServiceConstants() { }
}
