package dk.dbc.dataio.sink.marcconv;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "block")
@NamedQueries({
        @NamedQuery(
                name = ConversionBlock.GET_CONVERSION_BLOCKS_QUERY_NAME,
                query = ConversionBlock.GET_CONVERSION_BLOCKS_QUERY),
        @NamedQuery(
                name = ConversionBlock.DELETE_CONVERSION_BLOCKS_QUERY_NAME,
                query = ConversionBlock.DELETE_CONVERSION_BLOCKS_QUERY)
})
public class ConversionBlock {
    public static final String GET_CONVERSION_BLOCKS_QUERY =
            "SELECT block FROM ConversionBlock block" +
                    " WHERE block.key.jobId = ?1" +
                    " ORDER BY block.key.chunkId ASC";
    public static final String GET_CONVERSION_BLOCKS_QUERY_NAME =
            "ConversionBlock.getConversionBlocks";
    public static final String DELETE_CONVERSION_BLOCKS_QUERY =
            "DELETE FROM ConversionBlock block" +
                    " WHERE block.key.jobId = :jobId";
    public static final String DELETE_CONVERSION_BLOCKS_QUERY_NAME =
            "ConversionBlock.deleteConversionBlocks";

    @EmbeddedId
    private Key key;

    @Lob
    private byte[] bytes;

    public Key getKey() {
        return key;
    }

    public void setKey(Key key) {
        this.key = key;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    @Embeddable
    public static class Key {
        @Column(name = "jobid")
        private int jobId;

        @Column(name = "chunkid")
        private int chunkId;

        public Key(int jobId, int chunkId) {
            this.jobId = jobId;
            this.chunkId = chunkId;
        }

        public Key(long jobId, long chunkId) {
            // When someone changed the database keys from
            // biginteger to integer, they didn't follow through
            // and change the DTO fields as well (fx. in Chunk
            // and ChunkItem)
            this.jobId = (int) jobId;
            this.chunkId = (int) chunkId;
        }

        private Key() {
        }

        public int getJobId() {
            return jobId;
        }

        public int getChunkId() {
            return chunkId;
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
                    chunkId == key.chunkId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(jobId, chunkId);
        }

        @Override
        public String toString() {
            return "Key{" +
                    "jobId=" + jobId +
                    ", chunkId=" + chunkId +
                    '}';
        }
    }
}
