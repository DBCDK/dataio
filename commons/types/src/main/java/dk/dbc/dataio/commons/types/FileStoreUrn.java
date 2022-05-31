package dk.dbc.dataio.commons.types;

import dk.dbc.invariant.InvariantUtil;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Uniform Resource Name for file-store file with string representation
 * on the form '{@value #SCHEME}:{@value #TYPE}:fileId'
 */
public class FileStoreUrn {
    public static final String SCHEME = "urn";
    public static final String TYPE = "dataio-fs";
    public static final FileStoreUrn EMPTY_JOB_FILE = create("empty-job");

    private final String urn;
    private final String fileId;

    /**
     * Constructs a URI by parsing the given string
     *
     * @param urnString string to be parsed into a URN
     * @throws NullPointerException     if given null-valued urnString argument
     * @throws IllegalArgumentException if given empty-valued urnString argument
     * @throws URISyntaxException       if the given string does not match the format '{@value #SCHEME}:{@value #TYPE}:fileId'
     */
    public FileStoreUrn(String urnString) throws NullPointerException, IllegalArgumentException, URISyntaxException {
        final URI uri = new URI(InvariantUtil.checkNotNullNotEmptyOrThrow(urnString, "urnString"));
        if (!SCHEME.equals(uri.getScheme())) {
            throw new URISyntaxException(urnString, String.format("scheme is not %s", SCHEME));
        }
        final String[] parts = uri.getSchemeSpecificPart().split(":", 2);
        if (!TYPE.equals(parts[0])) {
            throw new URISyntaxException(urnString, String.format("type '%s' is not %s", parts[0], TYPE));
        }
        if (parts.length != 2) {
            throw new URISyntaxException(urnString, "no fileId in URN");
        }
        fileId = parts[1];
        urn = uri.toString();
    }

    public String getFileId() {
        return fileId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FileStoreUrn that = (FileStoreUrn) o;

        return urn.equals(that.urn);

    }

    @Override
    public int hashCode() {
        return urn.hashCode();
    }

    @Override
    public String toString() {
        return urn;
    }

    /**
     * Factory method for FileStoreUrn creation
     *
     * @param fileId file ID to be expressed as URN
     * @return FileStoreUrn instance
     * @throws NullPointerException     if given null-valued fileId argument
     * @throws IllegalArgumentException if given empty-valued fileId argument or if the given string
     *                                  does not match the format '{@value #SCHEME}:{@value #TYPE}:fileId'
     */
    public static FileStoreUrn create(String fileId) throws NullPointerException, IllegalArgumentException {
        try {
            InvariantUtil.checkNotNullNotEmptyOrThrow(fileId, "fileId");
            return new FileStoreUrn(String.format("%s:%s:%s", SCHEME, TYPE, fileId));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Unable to create FileStoreUrn", e);
        }
    }

    public static FileStoreUrn parse(String urn) throws NullPointerException, IllegalArgumentException {
        try {
            return new FileStoreUrn(urn);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid FileStoreUrn '" + urn + "'", e);
        }
    }
}
