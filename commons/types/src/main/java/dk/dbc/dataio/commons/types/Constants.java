package dk.dbc.dataio.commons.types;

public class Constants {
    public static final String MISSING_FIELD_VALUE = "__MISSING__";
    // I'm introducing the invariant that submitter number 1 indicates missing value.
    public static final long MISSING_SUBMITTER_VALUE = 1;

    public static final long PERSISTENCE_ID_LOWER_BOUND = 1L;
    public static final long PERSISTENCE_VERSION_LOWER_BOUND = 1L;
    public static final long CHUNK_ITEM_ID_LOWER_BOUND = -1L;
}
