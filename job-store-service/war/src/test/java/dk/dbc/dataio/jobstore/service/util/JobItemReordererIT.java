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

import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import dk.dbc.dataio.jobstore.service.partitioner.DataPartitionerResult;
import dk.dbc.dataio.jobstore.service.partitioner.JobItemReorderer;
import dk.dbc.dataio.jobstore.types.MarcRecordInfo;
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
        final DataPartitionerResult dataPartitionerResult = new DataPartitionerResult(null, recordInfo);

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
        Optional<DataPartitionerResult> next = persistenceContext.run(() -> reorderer.next(new DataPartitionerResult(null,
                recordInfoBuilder.parse(getMarcRecord(get001("deleted-head"), get004("h", "d"))).get())));
        assertThat("result of deleted head is present", next.isPresent(), is(true));
        assertThat("result of deleted head is empty", next.get().isEmpty(), is(true));

        next = persistenceContext.run(() -> reorderer.next(new DataPartitionerResult(null,
                recordInfoBuilder.parse(getMarcRecord(get001("deleted-volume"), get004("b", "d"))).get())));
        assertThat("result of deleted volume is present", next.isPresent(), is(true));
        assertThat("result of deleted volume is empty", next.get().isEmpty(), is(true));

        next = persistenceContext.run(() -> reorderer.next(new DataPartitionerResult(null,
                recordInfoBuilder.parse(getMarcRecord(get001("deleted-section"), get004("s", "d"))).get())));
        assertThat("result of deleted section is present", next.isPresent(), is(true));
        assertThat("result of deleted section is empty", next.get().isEmpty(), is(true));


        next = persistenceContext.run(() -> reorderer.next(new DataPartitionerResult(null,
                recordInfoBuilder.parse(getMarcRecord(get001("volume"), get004("b", "c"))).get())));
        assertThat("result of volume is present", next.isPresent(), is(true));
        assertThat("result of volume section is empty", next.get().isEmpty(), is(true));

        next = persistenceContext.run(() -> reorderer.next(new DataPartitionerResult(null,
                recordInfoBuilder.parse(getMarcRecord(get001("head"), get004("h", "c"))).get())));
        assertThat("result of head is present", next.isPresent(), is(true));
        assertThat("result of head section is empty", next.get().isEmpty(), is(true));

        next = persistenceContext.run(() -> reorderer.next(new DataPartitionerResult(null,
                recordInfoBuilder.parse(getMarcRecord(get001("section"), get004("s", "c"))).get())));
        assertThat("result of section is present", next.isPresent(), is(true));
        assertThat("result of section section is empty", next.get().isEmpty(), is(true));

        assertThat("Number of items persisted", getSizeOfTable(REORDERED_ITEM_TABLE_NAME), is(6L));

        persistenceContext.run(() -> {
                    DataPartitionerResult reordered = reorderer.next(DataPartitionerResult.EMPTY).orElse(null);
                    assertThat("reordered head", reordered.getRecordInfo().getId(), is("head"));

                    reordered = reorderer.next(DataPartitionerResult.EMPTY).orElse(null);
                    assertThat("reordered section", reordered.getRecordInfo().getId(), is("section"));

                    reordered = reorderer.next(DataPartitionerResult.EMPTY).orElse(null);
                    assertThat("reordered volume", reordered.getRecordInfo().getId(), is("volume"));

                    reordered = reorderer.next(DataPartitionerResult.EMPTY).orElse(null);
                    assertThat("reordered deleted volume", reordered.getRecordInfo().getId(), is("deleted-volume"));

                    reordered = reorderer.next(DataPartitionerResult.EMPTY).orElse(null);
                    assertThat("reordered deleted section", reordered.getRecordInfo().getId(), is("deleted-section"));

                    reordered = reorderer.next(DataPartitionerResult.EMPTY).orElse(null);
                    assertThat("reordered deleted head", reordered.getRecordInfo().getId(), is("deleted-head"));
                });

        assertThat("reorderer contains more items", reorderer.hasNext(), is(false));
        assertThat("Number of items remaining in persistent store", getSizeOfTable(REORDERED_ITEM_TABLE_NAME), is(0L));
    }
}
