/*
 * DataIO - Data IO
 *
 * Copyright (C) 2018 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.jobstore.service.partitioner;

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
 * Helper utility to transform a {@link DataPartitionerResult} into
 * a summarized form better suited for equality tests during unit testing
 */
public final class DataPartitionerResultTransformer {
    private DataPartitionerResultTransformer() {}

    /**
     * Transforms non-empty {@link DataPartitionerResult} into a {@link ResultSummary}
     * @param dataPartitionerResult result to be transformed
     * @return optional result summary
     */
    public static Optional<ResultSummary> toSummarizedResult(DataPartitionerResult dataPartitionerResult) {
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

    /**
     * Summarized form of {@link DataPartitionerResult}
     * better suited for equality tests during unit testing
     */
    public static class ResultSummary {
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
}
