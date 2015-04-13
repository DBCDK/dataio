package dk.dbc.dataio.commons.types.rest;

public class JobStoreServiceConstants {
    public static final String JOB_ID_VARIABLE = "jobId";
    public static final String CHUNK_ID_VARIABLE = "chunkId";

    public static final String JOB_COLLECTION               = "jobs";
    public static final String JOB_COLLECTION_SEARCHES      = "jobs/searches";
    public static final String ITEM_COLLECTION_SEARCHES     = "jobs/chunks/items/searches";
    public static final String JOB_CHUNK_PROCESSED          = "jobs/{jobId}/chunks/{chunkId}/processed";
    public static final String JOB_CHUNK_DELIVERED          = "jobs/{jobId}/chunks/{chunkId}/delivered";
    public static final String JOB_RESOURCEBUNDLE           = "jobs/{jobId}/resourcebundle";

    private JobStoreServiceConstants() { }
}
