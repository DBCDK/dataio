package dk.dbc.dataio.commons.types.rest;

public class JobStoreServiceConstants {
    public static final String JOB_ID_VARIABLE = "jobId";
    public static final String CHUNK_ID_VARIABLE = "chunkId";
    public static final String ITEM_ID_VARIABLE = "itemId";
    public static final String SINK_ID_VARIABLE = "sinkId";


    public static final String JOB_COLLECTION = "jobs";
    public static final String JOB_COLLECTION_ACCTESTS = "jobs/acctests";
    public static final String JOB_ABORT = "jobs/abort";
    public static final String JOB_RESEND = "dependency/retransmit";
    public static final String JOB_COLLECTION_EMPTY = "jobs/empty";
    public static final String JOB_COLLECTION_QUERIES = "jobs/queries";
    public static final String JOB_COLLECTION_COUNT = "jobs/count";
    public static final String ITEM_COLLECTION_QUERIES = "items/queries";
    public static final String ITEM_COLLECTION_COUNT = "items/count";
    public static final String JOB_COLLECTION_SEARCHES = "jobs/searches";
    public static final String JOB_COLLECTION_SEARCHES_COUNT = "jobs/searches/count";
    public static final String ITEM_COLLECTION_SEARCHES = "jobs/chunks/items/searches";
    public static final String ITEM_COLLECTION_SEARCHES_COUNT = "jobs/chunks/items/searches/count";
    public static final String JOB_CHUNK_PROCESSED = "jobs/{jobId}/chunks/{chunkId}/processed";
    public static final String JOB_CHUNK_DELIVERED = "jobs/{jobId}/chunks/{chunkId}/delivered";
    public static final String CHUNK_ITEM_PARTITIONED = "jobs/{jobId}/chunks/{chunkId}/items/{itemId}/partitioned";
    public static final String CHUNK_ITEM_PROCESSED = "jobs/{jobId}/chunks/{chunkId}/items/{itemId}/processed/current";
    public static final String CHUNK_ITEM_PROCESSED_NEXT = "jobs/{jobId}/chunks/{chunkId}/items/{itemId}/processed/next";
    public static final String CHUNK_ITEM_DELIVERED = "jobs/{jobId}/chunks/{chunkId}/items/{itemId}/delivered";
    public static final String JOB_NOTIFICATIONS = "jobs/{jobId}/notifications";
    public static final String JOB_WORKFLOW_NOTE = "jobs/{jobId}/workflownote";
    public static final String ITEM_WORKFLOW_NOTE = "jobs/{jobId}/chunks/{chunkId}/items/{itemId}/workflownote";
    public static final String JOB_CACHED_FLOW = "jobs/{jobId}/cachedflow";

    public static final String NOTIFICATIONS = "notifications";
    public static final String NOTIFICATIONS_TYPES_INVALID_TRNS = "notifications/types/invalidtrns";
    public static final String RERUNS = "reruns";

    public static final String EXPORT_ITEMS_PARTITIONED = "jobs/{jobId}/exports/items/partitioned";
    public static final String EXPORT_ITEMS_PROCESSED = "jobs/{jobId}/exports/items/processed";
    public static final String EXPORT_ITEMS_DELIVERED = "jobs/{jobId}/exports/items/delivered";
    public static final String EXPORT_ITEMS_PARTITIONED_FAILED = "jobs/{jobId}/exports/items/partitioned/failed";
    public static final String EXPORT_ITEMS_PROCESSED_FAILED = "jobs/{jobId}/exports/items/processed/failed";
    public static final String EXPORT_ITEMS_DELIVERED_FAILED = "jobs/{jobId}/exports/items/delivered/failed";
    public static final String ACTIVATE_JOB_PURGE = "jobs/purge";
    public static final String QUERY_PARAM_FORMAT = "format";

    public static final String SCHEDULER_SINK_FORCE_BULK_MODE = "dependency/sinks/{" + SINK_ID_VARIABLE + "}/forceBulkMode";
    public static final String SCHEDULER_SINK_FORCE_TRANSITION_MODE = "dependency/sinks/{" + SINK_ID_VARIABLE + "}/forceTransitionMode";
    public static final String FORCE_DEPENDENCY_TRACKING_RETRANSMIT = "dependency/retransmit";
    public static final String FORCE_DEPENDENCY_TRACKING_RETRANSMIT_ID = "dependency/retransmit/{jobId}";

    public static final String SINK_STATUS = "status/sinks/{sinkId}";
    public static final String SINKS_STATUS = "status/sinks";
    public static final String CLEAR_CACHE = "cache/clear";

    private JobStoreServiceConstants() {
    }
}
