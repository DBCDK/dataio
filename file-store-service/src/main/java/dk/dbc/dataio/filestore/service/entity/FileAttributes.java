package dk.dbc.dataio.filestore.service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import dk.dbc.invariant.InvariantUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

/**
 * Persistence domain class for file attributes objects where id is auto
 * generated by the underlying store
 */
@NamedNativeQueries({
        @NamedNativeQuery(
                name = FileAttributes.GET_FILES_FROM_METADATA,
                query = "SELECT * FROM file_attributes WHERE metadata::jsonb @> ?::jsonb ORDER BY id DESC",
                resultClass = FileAttributes.class),
        @NamedNativeQuery(
                name = FileAttributes.GET_FILES_FROM_METADATA_WITH_ORIGIN_OLDER_THAN,
                query = "SELECT * FROM file_attributes WHERE metadata::jsonb @> ?::jsonb AND creationtime<now()-CAST (? AS INTERVAL) ORDER BY id DESC",
                resultClass = FileAttributes.class),
        @NamedNativeQuery(
                name = FileAttributes.GET_FILES_NEVER_READ_OLDER_THAN,
                query = "SELECT * FROM file_attributes WHERE atime IS NULL AND creationtime<now()-CAST (? AS INTERVAL) ORDER BY id DESC",
                resultClass = FileAttributes.class)})

@Entity
@Table(name = FileAttributes.TABLE_NAME)
public class FileAttributes {
    public static final String TABLE_NAME = "file_attributes";
    public static final String GET_FILES_FROM_METADATA =
            "FileAttributes.getFilesFromMetadata";
    public static final String GET_FILES_FROM_METADATA_WITH_ORIGIN_OLDER_THAN =
            "FileAttributes.getFilesFromMetadataWithOriginOlderThan";
    public static final String GET_FILES_NEVER_READ_OLDER_THAN =
            "FileAttributes.getFilesNeverReadOlderThan";

    FileAttributes() {
    }

    /**
     * Class constructor
     *
     * @param creationTime file creation time
     * @param path         file location in store
     * @throws NullPointerException     if given null-valued creationTime or path argument
     * @throws IllegalArgumentException if given empty path
     */
    public FileAttributes(Date creationTime, Path path)
            throws NullPointerException, IllegalArgumentException {
        this.creationTime = new Date(InvariantUtil.checkNotNullOrThrow(creationTime, "creationTime").getTime());
        InvariantUtil.checkNotNullOrThrow(path, "path");
        location = InvariantUtil.checkNotNullNotEmptyOrThrow(path.toString(), "path");
    }

    @Id
    @SequenceGenerator(
            name = "fileattributes_id_seq",
            sequenceName = "fileattributes_id_seq",
            allocationSize = 1)
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "fileattributes_id_seq")
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    private Date creationTime;

    @Temporal(TemporalType.TIMESTAMP)
    private Date atime;

    @JsonIgnore
    private String location;

    private long byteSize;

    @JsonRawValue
    @Column(columnDefinition = "jsonb")
    @Convert(converter = String2JSonB.class)
    private String metadata;

    public Long getId() {
        return id;
    }

    public Date getCreationTime() {
        return new Date(creationTime.getTime());
    }

    public Date getAtime() {
        if (atime == null) {
            return null;
        }
        return new Date(atime.getTime());
    }

    public void setAtime(Date atime) {
        if (atime == null) {
            this.atime = null;
        } else {
            this.atime = new Date(atime.getTime());
        }
    }

    public Path getLocation() {
        return Paths.get(location);
    }

    public void setByteSize(long byteSize) {
        this.byteSize = byteSize;
    }

    public long getByteSize() {
        return byteSize;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}