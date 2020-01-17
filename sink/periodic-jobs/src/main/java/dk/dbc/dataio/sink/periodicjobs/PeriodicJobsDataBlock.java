/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.periodicjobs;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.util.Arrays;
import java.util.Objects;

@Entity
@Table(name = "datablock")
@NamedQueries({
    @NamedQuery(
            name = PeriodicJobsDataBlock.GET_DATA_BLOCKS_QUERY_NAME,
            query = PeriodicJobsDataBlock.GET_DATA_BLOCKS_QUERY),
    @NamedQuery(
            name = PeriodicJobsDataBlock.DELETE_DATA_BLOCKS_QUERY_NAME,
            query = PeriodicJobsDataBlock.DELETE_DATA_BLOCKS_QUERY)
})
public class PeriodicJobsDataBlock {
    public static final String GET_DATA_BLOCKS_QUERY =
            "SELECT datablock FROM PeriodicJobsDataBlock datablock" +
            " WHERE datablock.key.jobId = ?1" +
            " ORDER BY datablock.sortkey ASC";
    public static final String GET_DATA_BLOCKS_QUERY_NAME =
            "PeriodicJobsDataBlock.get";
    public static final String DELETE_DATA_BLOCKS_QUERY =
            "DELETE FROM PeriodicJobsDataBlock datablock" +
            " WHERE datablock.key.jobId = :jobId";
    public static final String DELETE_DATA_BLOCKS_QUERY_NAME =
            "PeriodicJobsDataBlock.delete";

    @EmbeddedId
    private Key key;

    private String sortkey;

    @Lob
	private byte[] bytes;

    public Key getKey() {
        return key;
    }

    public void setKey(Key key) {
        this.key = key;
    }

    public String getSortkey() {
        return sortkey;
    }

    public void setSortkey(String sortkey) {
        this.sortkey = sortkey;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PeriodicJobsDataBlock that = (PeriodicJobsDataBlock) o;

        if (!Objects.equals(key, that.key)) {
            return false;
        }
        if (!Objects.equals(sortkey, that.sortkey)) {
            return false;
        }
        return Arrays.equals(bytes, that.bytes);
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (sortkey != null ? sortkey.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(bytes);
        return result;
    }

    @Override
    public String toString() {
        return "PeriodicJobsDataBlock{" +
                "key=" + key +
                ", sortkey='" + sortkey + '\'' +
                ", bytes=" + Arrays.toString(bytes) +
                '}';
    }

    @Embeddable
    public static class Key {
        @Column(name = "jobid")
        private int jobId;

        @Column(name = "recordnumber")
        private int recordNumber;

        public Key(int jobId, int recordNumber) {
            this.jobId = jobId;
            this.recordNumber = recordNumber;
        }

        private Key() {}

        public int getJobId() {
            return jobId;
        }

        public int getRecordNumber() {
            return recordNumber;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Key key = (Key) o;
            return jobId == key.jobId &&
                    recordNumber == key.recordNumber;
        }

        @Override
        public int hashCode() {
            return Objects.hash(jobId, recordNumber);
        }

        @Override
        public String toString() {
            return "Key{" +
                    "jobId=" + jobId +
                    ", recordNumber=" + recordNumber +
                    '}';
        }
    }
}
