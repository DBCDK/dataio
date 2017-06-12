/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.entity.ReorderedItemEntity;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitionerResult;
import dk.dbc.dataio.jobstore.service.partitioner.JobItemReorderer;
import dk.dbc.dataio.jobstore.types.MarcRecordInfo;
import dk.dbc.dataio.jobstore.types.RecordInfo;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Optional;

import static dk.dbc.dataio.jobstore.service.util.MarcRecordInfoBuilderTest.get001;
import static dk.dbc.dataio.jobstore.service.util.MarcRecordInfoBuilderTest.get004;
import static dk.dbc.dataio.jobstore.service.util.MarcRecordInfoBuilderTest.getMarcRecord;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JobItemReordererIT extends AbstractJobStoreIT {
    private final int jobId = 42;
    private final MarcRecordInfoBuilder recordInfoBuilder = new MarcRecordInfoBuilder();
    private JobItemReorderer reorderer;

    @Before
    public void newInstance() {
        reorderer = new JobItemReorderer(jobId, entityManager);
    }

    @Test
    public void next_dataPartitionerResultContainsRecordOfTypeStandalone_passthrough() throws SQLException {
        final MarcRecordInfo recordInfo = recordInfoBuilder.parse(getMarcRecord(get001("id"), get004("e", "c"))).get();
        final DataPartitionerResult dataPartitionerResult = new DataPartitionerResult(null, recordInfo, 0);

        final Optional<DataPartitionerResult> next = persistenceContext.run(() -> reorderer.next(dataPartitionerResult));
        assertThat("DataPartitionerResult is present", next.isPresent(), is(true));
        assertThat("DataPartitionerResult is passed through", next.get(), is(dataPartitionerResult));
        assertThat("Number of items persisted", getSizeOfTable(REORDERED_ITEM_TABLE_NAME), is(0L));
    }

    @Test
    public void next_dataPartitionerResultArgIsEmptyAndNoItemsRemainsToBeReordered_returnsEmptyOptional() throws SQLException {
        final Optional<DataPartitionerResult> next = reorderer.next(DataPartitionerResult.EMPTY);
        assertThat(next.isPresent(), is(false));
        assertThat("Number of items persisted", getSizeOfTable(REORDERED_ITEM_TABLE_NAME), is(0L));
    }

    @Test
    public void sortOrder() throws SQLException {
        Optional<DataPartitionerResult> next = persistenceContext.run(() -> reorderer.next(new DataPartitionerResult(
                null, getDeletedRecordInfo("deleted-head", 'h'), 0)));
        assertThat("result of deleted head is present", next.isPresent(), is(true));
        assertThat("result of deleted head is empty", next.get().isEmpty(), is(true));

        next = persistenceContext.run(() -> reorderer.next(new DataPartitionerResult(
                null, getDeletedRecordInfo("deleted-volume", 'b'), 1)));
        assertThat("result of deleted volume is present", next.isPresent(), is(true));
        assertThat("result of deleted volume is empty", next.get().isEmpty(), is(true));

        next = persistenceContext.run(() -> reorderer.next(new DataPartitionerResult(
                null, getDeletedRecordInfo("deleted-section", 's'), 2)));
        assertThat("result of deleted section is present", next.isPresent(), is(true));
        assertThat("result of deleted section is empty", next.get().isEmpty(), is(true));


        next = persistenceContext.run(() -> reorderer.next(new DataPartitionerResult(
                null, getRecordInfo("volume", 'b'), 3)));
        assertThat("result of volume is present", next.isPresent(), is(true));
        assertThat("result of volume section is empty", next.get().isEmpty(), is(true));

        next = persistenceContext.run(() -> reorderer.next(new DataPartitionerResult(
                null, getRecordInfo("head", 'h'), 4)));
        assertThat("result of head is present", next.isPresent(), is(true));
        assertThat("result of head section is empty", next.get().isEmpty(), is(true));

        next = persistenceContext.run(() -> reorderer.next(new DataPartitionerResult(
                null, getRecordInfo("section", 's'), 5)));
        assertThat("result of section is present", next.isPresent(), is(true));
        assertThat("result of section section is empty", next.get().isEmpty(), is(true));

        assertThat("Number of items persisted", getSizeOfTable(REORDERED_ITEM_TABLE_NAME), is(6L));
        assertThat("Number of items to be reordered", reorderer.getNumberOfItems(), is(6));

        persistenceContext.run(() -> {
                    DataPartitionerResult reordered = reorderer.next(DataPartitionerResult.EMPTY).orElse(null);
                    assertThat("reordered head", reordered.getRecordInfo().getId(), is("head"));
                    assertThat("reordered head position in datafile", reordered.getPositionInDatafile(), is(4));

                    reordered = reorderer.next(DataPartitionerResult.EMPTY).orElse(null);
                    assertThat("reordered section", reordered.getRecordInfo().getId(), is("section"));
                    assertThat("reordered section position in datafile", reordered.getPositionInDatafile(), is(5));

                    reordered = reorderer.next(DataPartitionerResult.EMPTY).orElse(null);
                    assertThat("reordered volume", reordered.getRecordInfo().getId(), is("volume"));
                    assertThat("reordered volume position in datafile", reordered.getPositionInDatafile(), is(3));

                    reordered = reorderer.next(DataPartitionerResult.EMPTY).orElse(null);
                    assertThat("reordered deleted volume", reordered.getRecordInfo().getId(), is("deleted-volume"));
                    assertThat("reordered deleted volume position in datafile", reordered.getPositionInDatafile(), is(1));

                    reordered = reorderer.next(DataPartitionerResult.EMPTY).orElse(null);
                    assertThat("reordered deleted section", reordered.getRecordInfo().getId(), is("deleted-section"));
                    assertThat("reordered deleted section position in datafile", reordered.getPositionInDatafile(), is(2));

                    reordered = reorderer.next(DataPartitionerResult.EMPTY).orElse(null);
                    assertThat("reordered deleted head", reordered.getRecordInfo().getId(), is("deleted-head"));
                    assertThat("reordered deleted head position in datafile", reordered.getPositionInDatafile(), is(0));
                });

        assertThat("reorderer contains more items", reorderer.hasNext(), is(false));
        assertThat("Number of items remaining in persistent store", getSizeOfTable(REORDERED_ITEM_TABLE_NAME), is(0L));
        assertThat("Number of items to be reordered after iterations", reorderer.getNumberOfItems(), is(0));
    }

    @Test
    public void numberOfItemsInitializedFromDatabase() {
        persist(new ReorderedItemEntity()
                .withJobId(jobId)
                .withSortkey(JobItemReorderer.SortOrder.VOLUME.getIntValue())
                .withChunkItem(new ChunkItemBuilder().build())
                .withRecordInfo(new MarcRecordInfo("id1", MarcRecordInfo.RecordType.VOLUME, false, null)));
        persist(new ReorderedItemEntity()
                .withJobId(jobId)
                .withSortkey(JobItemReorderer.SortOrder.VOLUME.getIntValue())
                .withChunkItem(new ChunkItemBuilder().build())
                .withRecordInfo(new MarcRecordInfo("id2", MarcRecordInfo.RecordType.VOLUME, false, null)));
        assertThat(new JobItemReorderer(jobId, entityManager).getNumberOfItems(), is(2));
    }

    private RecordInfo getRecordInfo(String bibliographicRecordId, char recordType) {
        return recordInfoBuilder.parse(getMarcRecord(
                get001(bibliographicRecordId),
                get004(String.valueOf(recordType), "c")))
                .get();
    }

    private RecordInfo getDeletedRecordInfo(String bibliographicRecordId, char recordType) {
        return recordInfoBuilder.parse(getMarcRecord(
                get001(bibliographicRecordId),
                get004(String.valueOf(recordType), "d")))
                .get();
    }
}
