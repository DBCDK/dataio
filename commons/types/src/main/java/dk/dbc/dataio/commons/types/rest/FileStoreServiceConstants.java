package dk.dbc.dataio.commons.types.rest;

public class FileStoreServiceConstants {
    public static final String FILE_ID_VARIABLE = "id";

    public static final String FILES_COLLECTION = "files";
    public static final String FILE = "files/{id}";
    public static final String FILE_ATTRIBUTES = "files/{id}/attributes";
    public static final String FILE_ATTRIBUTES_BYTESIZE = "files/{id}/attributes/bytesize";

    private FileStoreServiceConstants() {
    }
}
