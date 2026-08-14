package dk.dbc.dataio.jobstore.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "sink_record_delivery_watermark")
public class WatermarkEntity {
    @EmbeddedId
    private Key key;

    @Column(name = "job_id")
    private int jobId;

    @Column(name = "chunk_id")
    private int chunkId;

    @Column(name = "item_id")
    private short itemId;

    /* insertable/updatable = false: this column is never written through JPA. On
       insert the DB's own DEFAULT now() applies. The only writer once the delivery
       upsert lands is a hand-authored native "INSERT ... ON CONFLICT DO UPDATE"
       statement (see docs/chunk-scheduling-redesign.md, "Upsert on delivery") that
       bumps last_modified only when the watermark actually advances; these flags
       only constrain EclipseLink-generated SQL, so they don't affect that native
       statement. They do stop this column from being silently overwritten by an
       unrelated JPA merge()/flush() on this entity. */
    @Column(name = "last_modified", insertable = false, updatable = false)
    private Timestamp lastModified;

    public Key getKey() {
        return key;
    }

    public WatermarkEntity withKey(Key key) {
        this.key = key;
        return this;
    }

    public int getJobId() {
        return jobId;
    }

    public WatermarkEntity withJobId(int jobId) {
        this.jobId = jobId;
        return this;
    }

    public int getChunkId() {
        return chunkId;
    }

    public WatermarkEntity withChunkId(int chunkId) {
        this.chunkId = chunkId;
        return this;
    }

    public short getItemId() {
        return itemId;
    }

    public WatermarkEntity withItemId(short itemId) {
        this.itemId = itemId;
        return this;
    }

    public Timestamp getLastModified() {
        return lastModified;
    }

    @Embeddable
    public static class Key implements Serializable {
        @Column(name = "sink_id")
        private int sinkId;

        @Column(name = "record_key")
        private String recordKey;

        /* Private constructor in order to keep class static */
        private Key() {
        }

        public Key(int sinkId, String recordKey) {
            this.sinkId = sinkId;
            this.recordKey = recordKey;
        }

        public int getSinkId() {
            return sinkId;
        }

        public String getRecordKey() {
            return recordKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key key = (Key) o;
            return sinkId == key.sinkId && Objects.equals(recordKey, key.recordKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sinkId, recordKey);
        }

        @Override
        public String toString() {
            return "Key{" +
                    "sinkId=" + sinkId +
                    ", recordKey='" + recordKey + '\'' +
                    '}';
        }
    }
}
