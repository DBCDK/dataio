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

package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.jobstore.service.entity.ReorderedItemEntity;
import dk.dbc.dataio.jobstore.service.util.MarcRecordInfoBuilder;
import dk.dbc.dataio.jobstore.types.MarcRecordInfo;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.Optional;

import static dk.dbc.dataio.jobstore.service.util.MarcRecordInfoBuilderTest.get001;
import static dk.dbc.dataio.jobstore.service.util.MarcRecordInfoBuilderTest.get004;
import static dk.dbc.dataio.jobstore.service.util.MarcRecordInfoBuilderTest.getMarcRecord;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JobItemReordererTest {
    private final int jobId = 42;
    private final EntityManager entityManager = mock(EntityManager.class);
    private final MarcRecordInfoBuilder recordInfoBuilder = new MarcRecordInfoBuilder();

    private JobItemReorderer reorderer;

    @Before
    public void newInstance() {
        reorderer = new JobItemReorderer(jobId, entityManager);
    }

    @Test
    public void hasNext_beforeFirstCallOfNext_returnsFalse() {
        assertThat(reorderer.hasNext(), is(false));
    }

    @Test
    public void next_dataPartitionerResultContainsRecordOfTypeHead_persistsAndReturnsEmptyPartitionerResult() {
        final MarcRecordInfo recordInfo = recordInfoBuilder.parse(getMarcRecord(get001("id"), get004("h", "c"))).get();
        final DataPartitionerResult dataPartitionerResult = new DataPartitionerResult(null, recordInfo);

        final Optional<DataPartitionerResult> next = reorderer.next(dataPartitionerResult);
        assertThat("DataPartitionerResult is present", next.isPresent(), is(true));
        assertThat("DataPartitionerResult is empty", next.get().isEmpty(), is(true));

        verify(entityManager).persist(any(ReorderedItemEntity.class));
    }

    @Test
    public void next_dataPartitionerResultContainsDeleteMarkedRecordOfTypeHead_persistsAndReturnsEmptyPartitionerResult() {
        final MarcRecordInfo recordInfo = recordInfoBuilder.parse(getMarcRecord(get001("id"), get004("h", "d"))).get();
        final DataPartitionerResult dataPartitionerResult = new DataPartitionerResult(null, recordInfo);

        final Optional<DataPartitionerResult> next = reorderer.next(dataPartitionerResult);
        assertThat("DataPartitionerResult is present", next.isPresent(), is(true));
        assertThat("DataPartitionerResult is empty", next.get().isEmpty(), is(true));

        verify(entityManager).persist(any(ReorderedItemEntity.class));
    }

    @Test
    public void next_dataPartitionerResultContainsRecordOfTypeSection_persistsAndReturnsEmptyPartitionerResult() {
        final MarcRecordInfo recordInfo = recordInfoBuilder.parse(getMarcRecord(get001("id"), get004("s", "c"))).get();
        final DataPartitionerResult dataPartitionerResult = new DataPartitionerResult(null, recordInfo);

        final Optional<DataPartitionerResult> next = reorderer.next(dataPartitionerResult);
        assertThat("DataPartitionerResult is present", next.isPresent(), is(true));
        assertThat("DataPartitionerResult is empty", next.get().isEmpty(), is(true));

        verify(entityManager).persist(any(ReorderedItemEntity.class));
    }

    @Test
    public void next_dataPartitionerResultContainsDeleteMarkedRecordOfTypeSection_persistsAndReturnsEmptyPartitionerResult() {
        final MarcRecordInfo recordInfo = recordInfoBuilder.parse(getMarcRecord(get001("id"), get004("s", "d"))).get();
        final DataPartitionerResult dataPartitionerResult = new DataPartitionerResult(null, recordInfo);

        final Optional<DataPartitionerResult> next = reorderer.next(dataPartitionerResult);
        assertThat("DataPartitionerResult is present", next.isPresent(), is(true));
        assertThat("DataPartitionerResult is empty", next.get().isEmpty(), is(true));

        verify(entityManager).persist(any(ReorderedItemEntity.class));
    }

    @Test
    public void next_dataPartitionerResultContainsRecordOfTypeVolume_persistsAndReturnsEmptyPartitionerResult() {
        final MarcRecordInfo recordInfo = recordInfoBuilder.parse(getMarcRecord(get001("id"), get004("b", "c"))).get();
        final DataPartitionerResult dataPartitionerResult = new DataPartitionerResult(null, recordInfo);

        final Optional<DataPartitionerResult> next = reorderer.next(dataPartitionerResult);
        assertThat("DataPartitionerResult is present", next.isPresent(), is(true));
        assertThat("DataPartitionerResult is empty", next.get().isEmpty(), is(true));

        verify(entityManager).persist(any(ReorderedItemEntity.class));
    }

    @Test
    public void next_dataPartitionerResultContainsDeleteMarkedRecordOfTypeVolume_persistsAndReturnsEmptyPartitionerResult() {
        final MarcRecordInfo recordInfo = recordInfoBuilder.parse(getMarcRecord(get001("id"), get004("b", "d"))).get();
        final DataPartitionerResult dataPartitionerResult = new DataPartitionerResult(null, recordInfo);

        final Optional<DataPartitionerResult> next = reorderer.next(dataPartitionerResult);
        assertThat("DataPartitionerResult is present", next.isPresent(), is(true));
        assertThat("DataPartitionerResult is empty", next.get().isEmpty(), is(true));

        verify(entityManager).persist(any(ReorderedItemEntity.class));
    }

    @Test
    public void next_dataPartitionerResultContainsRecordOfTypeStandalone_passthrough() {
        final MarcRecordInfo recordInfo = recordInfoBuilder.parse(getMarcRecord(get001("id"), get004("e", "c"))).get();
        final DataPartitionerResult dataPartitionerResult = new DataPartitionerResult(null, recordInfo);

        final Optional<DataPartitionerResult> next = reorderer.next(dataPartitionerResult);
        assertThat("DataPartitionerResult is present", next.isPresent(), is(true));
        assertThat("DataPartitionerResult is passed through", next.get(), is(dataPartitionerResult));

        verify(entityManager, times(0)).persist(any(ReorderedItemEntity.class));
    }

    @Test
    public void next_dataPartitionerResultArgIsEmptyAndNoItemsRemainsToBeReordered_returnsEmptyOptional() {
        final Optional<DataPartitionerResult> next = reorderer.next(DataPartitionerResult.EMPTY);
        assertThat(next.isPresent(), is(false));

        verify(entityManager, times(0)).persist(any(ReorderedItemEntity.class));
    }

    @Test
    public void next_dataPartitionerResultArgIsEmptyAndItemsRemainsToBeReordered_returnsReorderedDataPartitionerResult() {
        final DataPartitionerResult reorderedResult = new DataPartitionerResult(
                null, recordInfoBuilder.parse(getMarcRecord(get001("id"), get004("b", "d"))).get());

        final ReorderedItemEntity entity = new ReorderedItemEntity();
        entity.setRecordInfo((MarcRecordInfo) reorderedResult.getRecordInfo());

        when(entityManager.find(eq(ReorderedItemEntity.class), anyInt())).thenReturn(entity);

        reorderer.next(reorderedResult);
        assertThat("Reorderer has result to be reordered", reorderer.hasNext(), is(true));

        final Optional<DataPartitionerResult> next = reorderer.next(DataPartitionerResult.EMPTY);
        assertThat("DataPartitionerResult is present", next.isPresent(), is(true));
        assertThat("DataPartitionerResult is reordered", next.get(), is(reorderedResult));

        assertThat("Reorderer has result to be reordered", reorderer.hasNext(), is(false));
    }

    @Test
    public void sortOrders() {
        assertThat("Head comes before section",
                JobItemReorderer.SortOrder.HEAD.getIntValue() < JobItemReorderer.SortOrder.SECTION.getIntValue(), is(true));
        assertThat("Section comes before volume",
                JobItemReorderer.SortOrder.SECTION.getIntValue() < JobItemReorderer.SortOrder.VOLUME.getIntValue(), is(true));
        assertThat("Volume comes before deleted volume",
                JobItemReorderer.SortOrder.VOLUME.getIntValue() < JobItemReorderer.SortOrder.VOLUME_DELETE.getIntValue(), is(true));
        assertThat("Deleted volume comes before deleted section",
                JobItemReorderer.SortOrder.VOLUME_DELETE.getIntValue() < JobItemReorderer.SortOrder.SECTION_DELETE.getIntValue(), is(true));
        assertThat("Deleted section comes before deleted head",
                JobItemReorderer.SortOrder.SECTION_DELETE.getIntValue() < JobItemReorderer.SortOrder.HEAD_DELETE.getIntValue(), is(true));
    }
}