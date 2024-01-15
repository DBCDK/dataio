package dk.dbc.dataio.commons.partioner;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.jobstore.types.RecordInfo;

/**
 * This class encapsulates a data partitioner result containing the actual chunk item as
 * well as record meta data
 */
public class DataPartitionerResult {
    public static final DataPartitionerResult EMPTY = new DataPartitionerResult(null, null, 0);
    private final ChunkItem chunkItem;
    private final RecordInfo recordInfo;
    private final int positionInDatafile;

    public DataPartitionerResult(ChunkItem chunkItem, RecordInfo recordInfo, int positionInDatafile) {
        this.chunkItem = chunkItem;
        this.recordInfo = recordInfo;
        this.positionInDatafile = positionInDatafile;
    }

    public ChunkItem getChunkItem() {
        return chunkItem;
    }

    public RecordInfo getRecordInfo() {
        return recordInfo;
    }

    public int getPositionInDatafile() {
        return positionInDatafile;
    }

    public boolean isEmpty() {
        return chunkItem == null && recordInfo == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DataPartitionerResult that = (DataPartitionerResult) o;

        if (positionInDatafile != that.positionInDatafile) {
            return false;
        }
        if (chunkItem != null ? !chunkItem.equals(that.chunkItem) : that.chunkItem != null) {
            return false;
        }
        return recordInfo != null ? recordInfo.equals(that.recordInfo) : that.recordInfo == null;
    }

    @Override
    public int hashCode() {
        int result = chunkItem != null ? chunkItem.hashCode() : 0;
        result = 31 * result + (recordInfo != null ? recordInfo.hashCode() : 0);
        result = 31 * result + positionInDatafile;
        return result;
    }

    @Override
    public String toString() {
        return "DataPartitionerResult{" + "chunkItem=" + chunkItem + ", recordInfo=" + recordInfo + ", positionInDatafile=" + positionInDatafile + '}';
    }
}
