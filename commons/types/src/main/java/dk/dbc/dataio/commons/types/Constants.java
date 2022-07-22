package dk.dbc.dataio.commons.types;

public class Constants {
    public static final short CHUNK_MAX_SIZE = 10;

    public static final String SINK_ID_ENV_VARIABLE = "SINK_ID";
    public static final String PROCESSOR_SHARD_ENV_VARIABLE = "PROCESSOR_SHARD";
    public static final String MISSING_FIELD_VALUE = "__MISSING__";
    public static final String CALL_OPEN_AGENCY = "__CALL_OPEN_AGENCY__";
    public static final String UPDATE_VALIDATE_ONLY_FLAG = "UPDATE_VALIDATE_ONLY_FLAG";
    public static final String JOBTYPE_PERSISTENT = "PERSISTENT";
    public static final String JOBTYPE_TRANSIENT = "TRANSIENT";
    public static final String JOBTYPE_SUPER_TRANSIENT = "SUPER_TRANSIENT";

    // I'm introducing the invariant that submitter number 1 indicates missing value.
    public static final long MISSING_SUBMITTER_VALUE = 1;
    public static final long PERSISTENCE_ID_LOWER_BOUND = 1L;
    public static final long PERSISTENCE_VERSION_LOWER_BOUND = 1L;
    public static final long CHUNK_ITEM_ID_LOWER_BOUND = -1L;
}
