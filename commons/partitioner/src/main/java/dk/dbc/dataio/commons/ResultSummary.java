package dk.dbc.dataio.commons;

import dk.dbc.dataio.commons.partioner.DataPartitionerResult;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.marc.reader.MarcXchangeV1Reader;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static dk.dbc.marc.binding.DataField.hasSubFieldCode;
import static dk.dbc.marc.binding.MarcRecord.hasTag;

/**
 * Summarized form of {@link DataPartitionerResult}
 * better suited for equality tests during unit testing
 */
public final class ResultSummary {
    /**
     * Transforms non-empty {@link DataPartitionerResult} into a {@link ResultSummary}
     *
     * @param dataPartitionerResult result to be transformed
     * @return optional result summary
     */
    public static Optional<ResultSummary> of(DataPartitionerResult dataPartitionerResult) {
        if (dataPartitionerResult != null && !dataPartitionerResult.isEmpty()) {
            final ChunkItem chunkItem = dataPartitionerResult.getChunkItem();
            if (chunkItem != null) {
                final ResultSummary resultSummary = new ResultSummary()
                        .withStatus(chunkItem.getStatus());
                if (chunkItem.getStatus() == ChunkItem.Status.SUCCESS) {
                    resultSummary.withIds(extractIdsFromMarcRecords(chunkItem));
                }
                return Optional.of(resultSummary);
            }
        }
        return Optional.empty();
    }

    private static List<String> extractIdsFromMarcRecords(ChunkItem chunkItem) {
        try {
            final List<String> ids = new ArrayList<>();
            final MarcXchangeV1Reader reader = new MarcXchangeV1Reader(
                    new ByteArrayInputStream(chunkItem.getData()), chunkItem.getEncoding());
            MarcRecord marcRecord = reader.read();
            while (marcRecord != null) {
                marcRecord.getField(DataField.class, hasTag("001"))
                        .ifPresent(f001 -> f001.getSubField(hasSubFieldCode('a'))
                                .ifPresent(a -> ids.add(a.getData())));
                marcRecord = reader.read();
            }
            return ids;
        } catch (MarcReaderException e) {
            throw new RuntimeException("Error extracting IDs from MARC records", e);
        }
    }

    public ResultSummary() {
    }

    private ChunkItem.Status status;
    private List<String> ids = new ArrayList<>();

    public ChunkItem.Status getStatus() {
        return status;
    }

    public ResultSummary withStatus(ChunkItem.Status status) {
        this.status = status;
        return this;
    }

    public List<String> getIds() {
        return ids;
    }

    public ResultSummary withIds(List<String> ids) {
        if (ids != null) {
            this.ids = ids;
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ResultSummary that = (ResultSummary) o;

        if (status != that.status) {
            return false;
        }
        return Objects.equals(ids, that.ids);
    }

    @Override
    public int hashCode() {
        int result = status != null ? status.hashCode() : 0;
        result = 31 * result + (ids != null ? ids.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SummarizedResult{" +
                "status=" + status +
                ", ids=" + ids +
                '}';
    }
}
