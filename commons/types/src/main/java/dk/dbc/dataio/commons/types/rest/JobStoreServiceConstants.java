package dk.dbc.dataio.commons.types.rest;

public class JobStoreServiceConstants {
    public static final String JOB_ID = "jobId";
    public static final String STATUS = "status";
    public static final String CHUNK_ID_VARIABLE = "chunkId";
    public static final String ITEM_ID_VARIABLE = "itemId";
    public static final String SINK_ID_VARIABLE = "sinkId";
    public static final String RECORD_KEY_QUERY_PARAM = "recordKey";


    public static final String JOB_COLLECTION = "jobs";
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
    public static final String CHECK_INCOMPLETE = "jobs/check_incomplete/{days}";
    public static final String ACTIVATE_JOB_PURGE = "jobs/purge";
    public static final String QUERY_PARAM_FORMAT = "format";

    public static final String SCHEDULER_SINK_FORCE_BULK_MODE = "dependency/sinks/{" + SINK_ID_VARIABLE + "}/forceBulkMode";
    public static final String SCHEDULER_SINK_FORCE_TRANSITION_MODE = "dependency/sinks/{" + SINK_ID_VARIABLE + "}/forceTransitionMode";
    public static final String FORCE_DEPENDENCY_TRACKING_RETRANSMIT = "dependency/retransmit";
    public static final String FORCE_DEPENDENCY_TRACKING_RETRANSMIT_ID = "dependency/retransmit/{jobIds}";
    /**
     * Runs the per-job gate sweep that {@code AdminBean.recheckBlocks} otherwise only runs hourly.
     * The sweep is a recovery mechanism, so it has to be reachable when a gate is actually stranded
     * rather than only at minute 10 of the next hour.
     */
    public static final String DEPENDENCY_GATE_SWEEP = "dependency/gate_sweep";

    /**
     * Runs the whole of {@code AdminBean.recheckBlocks} rather than only the gate sweep half of it.
     * <p>
     * The half {@link #DEPENDENCY_GATE_SWEEP} does not reach is the one that drops the scheduling
     * rows of jobs that are gone or already completed, lifts the barrier of each, and recounts the
     * sink status map. That is where the recheck's own nested transactions are, so it is the half
     * whose failure mode is an undetectable hang, and until this existed it was reachable only by
     * waiting for minute 10 of the next hour.
     */
    public static final String DEPENDENCY_RECHECK_BLOCKS = "dependency/recheck_blocks";

    /**
     * Runs the two sweeps that re-drive a chunk nothing else is watching, which otherwise run only
     * once a minute.
     * <p>
     * {@code AdminBean.updateStaleChunks} rescues a chunk whose dispatch attempt was fired and
     * never arrived, and re-drives one whose phase finished without its row moving.
     * {@code JobSchedulerBulkSubmitterBean.sweepSinksWithParkedChunks} then dispatches for the
     * sinks the table says hold parked chunks, which is what reaches a chunk the sink chunk counts
     * have lost. An operator with a stranded chunk wants both, so this runs both.
     * <p>
     * The dispatch each sweep triggers is asynchronous and runs in its own transaction, so a chunk
     * this call rescues is sent shortly after it returns rather than during it.
     */
    public static final String DEPENDENCY_STALE_SWEEP = "dependency/stale_sweep";
    public static final String DEPENDENCIES = "dependencies/{jobId}";

    public static final String SINK_STATUS = "status/sinks/{sinkId}";
    public static final String SINK_WATERMARK = "sinks/{" + SINK_ID_VARIABLE + "}/watermarks";
    public static final String SINKS_STATUS = "status/sinks";
    public static final String SINKS_STATUS_RECOUNT = "status/sinks/recount";
    public static final String CLEAR_CACHE = "cache/clear";
    public static final String CLEAR_HZ = "cache/clear_hz/{name}";

    private JobStoreServiceConstants() {
    }
}
