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

package dk.dbc.dataio.sink.diff;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.sink.testutil.ObjectFactory;
import dk.dbc.dataio.sink.types.SinkException;
import dk.dbc.javascript.recordprocessing.FailRecord;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Ignore
public class DiffMessageProcessorBeanTest extends AbstractDiffGeneratorTest {
    private final static String DBC_TRACKING_ID = "dataio_";

    private final JobStoreServiceConnectorBean jobStoreServiceConnectorBean = mock(JobStoreServiceConnectorBean.class);
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);

    @Before
    public void setupMocks() {
        when(jobStoreServiceConnectorBean.getConnector()).thenReturn(jobStoreServiceConnector);
    }

    @Test
    public void sendsResultToJobStore() throws ServiceException, InvalidMessageException, JobStoreServiceConnectorException {
        final ConsumedMessage message = ObjectFactory.createConsumedMessage(new ChunkBuilder(Chunk.Type.PROCESSED).build());
        getDiffMessageProcessorBean().handleConsumedMessage(message);

        verify(jobStoreServiceConnector).addChunkIgnoreDuplicates(any(Chunk.class), anyLong(), anyLong());
    }

    @Test
    public void failOnMissingNextItems() throws SinkException {
        final List<ChunkItem> chunkItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setTrackingId(DBC_TRACKING_ID + 1).setStatus(ChunkItem.Status.FAILURE).build(),
                new ChunkItemBuilder().setId(1L).setTrackingId(DBC_TRACKING_ID + 2).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setTrackingId(DBC_TRACKING_ID + 3).setStatus(ChunkItem.Status.IGNORE).build()
        );
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setItems(chunkItems)
                .build();

        final Chunk result = getDiffMessageProcessorBean().processPayload(chunk);
        assertThat("number of chunk items in result", result.size(), is(chunkItems.size()));

        final Iterator<ChunkItem> iterator = result.iterator();

        ChunkItem item = iterator.next();
        assertThat("1st item status", item.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("1st item diagnostic", item.getDiagnostics().size(), is(1));
        assertThat("1st item trackingId", item.getTrackingId(), is(DBC_TRACKING_ID+ 1));

        item = iterator.next();
        assertThat("2nd item status", item.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("2nd item diagnostics", item.getDiagnostics().size(), is(1));
        assertThat("2nd item trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 2));

        item = iterator.next();
        assertThat("3rd item status", item.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("3rd item diagnostics", item.getDiagnostics().size(), is(1));
        assertThat("3rd item trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 3));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void failOnXmlDiff() throws SinkException {
        final List<ChunkItem> currentItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setTrackingId(DBC_TRACKING_ID + 1).setData(AddiDiffGeneratorTest.XML_CONTENT).setStatus(ChunkItem.Status.FAILURE).build(),
                new ChunkItemBuilder().setId(1L).setTrackingId(DBC_TRACKING_ID + 2).setData(AddiDiffGeneratorTest.XML_CONTENT).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setTrackingId(DBC_TRACKING_ID + 3).setData(AddiDiffGeneratorTest.XML_CONTENT).setStatus(ChunkItem.Status.IGNORE).build(),
                new ChunkItemBuilder().setId(3L).setTrackingId(DBC_TRACKING_ID + 4).setData(AddiDiffGeneratorTest.XML_CONTENT).setStatus(ChunkItem.Status.SUCCESS).build());
        final List<ChunkItem> nextItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setData(AddiDiffGeneratorTest.XML_CONTENT_NEXT).setStatus(ChunkItem.Status.FAILURE).build(),
                new ChunkItemBuilder().setId(1L).setData(AddiDiffGeneratorTest.XML_CONTENT_NEXT).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setData(AddiDiffGeneratorTest.XML_CONTENT).setStatus(ChunkItem.Status.IGNORE).build(),
                new ChunkItemBuilder().setId(3L).setData(AddiDiffGeneratorTest.XML_CONTENT).setStatus(ChunkItem.Status.SUCCESS).build());
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setItems(currentItems)
                .setNextItems(nextItems)
                .build();

        final Chunk result = getDiffMessageProcessorBean().processPayload(chunk);
        assertThat("number of chunk items in result", result.size(), is(currentItems.size()));

        final Iterator<ChunkItem> iterator = result.iterator();

        ChunkItem item = iterator.next();
        assertThat("1st item status", item.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("1st item diagnostics", item.getDiagnostics(), is(nullValue()));
        assertThat("1st item trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 1));

        item = iterator.next();
        assertThat("2nd item status", item.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("2nd item diagnostics", item.getDiagnostics().size(), is(1));
        assertThat("2nd item trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 2));

        item = iterator.next();
        assertThat("3rd item status", item.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("3rd item diagnostics", item.getDiagnostics(), is(nullValue()));
        assertThat("3rd item trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 3));

        item = iterator.next();
        assertThat("4th item status", item.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("4th item diagnostics", item.getDiagnostics(), is(nullValue()));
        assertThat("4th item trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 4));

        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void failOnAddiDiff() throws SinkException {
        final byte[] addi1 = AddiDiffGeneratorTest.getAddiRecord(AddiDiffGeneratorTest.XML_METADATA, AddiDiffGeneratorTest.XML_CONTENT).getBytes();
        final byte[] addi2 = AddiDiffGeneratorTest.getAddiRecord(AddiDiffGeneratorTest.XML_METADATA_NEXT, AddiDiffGeneratorTest.XML_CONTENT).getBytes();
        final byte[] addi3 = AddiDiffGeneratorTest.getAddiRecord(AddiDiffGeneratorTest.XML_METADATA, AddiDiffGeneratorTest.XML_CONTENT_NEXT).getBytes();
        final byte[] addi4 = AddiDiffGeneratorTest.getAddiRecord(AddiDiffGeneratorTest.XML_METADATA_NEXT, AddiDiffGeneratorTest.XML_CONTENT_NEXT).getBytes();
        final byte[] addi5 = AddiDiffGeneratorTest.getAddiRecord(AddiDiffGeneratorTest.XML_METADATA, "<invalid>").getBytes();

        final List<ChunkItem> currentItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setTrackingId(DBC_TRACKING_ID + 1).setData(addi1).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(1L).setTrackingId(DBC_TRACKING_ID + 2).setData(addi1).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setTrackingId(DBC_TRACKING_ID + 3).setData(addi1).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(3L).setTrackingId(DBC_TRACKING_ID + 4).setData(addi1).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(4L).setTrackingId(DBC_TRACKING_ID + 5).setData(addi1).setStatus(ChunkItem.Status.SUCCESS).build());

        final List<ChunkItem> nextItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setData(addi1).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(1L).setData(addi2).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setData(addi3).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(3L).setData(addi4).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(4L).setData(addi5).setStatus(ChunkItem.Status.SUCCESS).build());

        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setItems(currentItems)
                .setNextItems(nextItems)
                .build();

        final Chunk result = getDiffMessageProcessorBean().processPayload(chunk);
        assertThat("number of chunk items in result", result.size(), is(currentItems.size()));

        final Iterator<ChunkItem> iterator = result.iterator();

        ChunkItem item = iterator.next();
        assertThat("1st item status", item.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("1st item diagnostics", item.getDiagnostics(), is(nullValue()));
        assertThat("1st item trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 1));

        item = iterator.next();
        assertThat("2nd item status", item.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("2nd item diagnostics", item.getDiagnostics().size(), is(1));
        assertThat("2nd item trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 2));

        item = iterator.next();
        assertThat("3rd item status", item.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("3rd item diagnostics", item.getDiagnostics().size(), is(1));
        assertThat("3rd item trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 3));

        item = iterator.next();
        assertThat("4th item status", item.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("4th item diagnostics", item.getDiagnostics().size(), is(1));
        assertThat("4th item trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 4));

        item = iterator.next();
        assertThat("5th item status", item.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("5th item diagnostics", item.getDiagnostics().size(), is(1));
        assertThat("5th item trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 5));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void failOnStatusDiff() throws SinkException {
        final List<ChunkItem> currentItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setTrackingId(DBC_TRACKING_ID + 1).setData(AddiDiffGeneratorTest.XML_CONTENT).setStatus(ChunkItem.Status.FAILURE).build(),
                new ChunkItemBuilder().setId(1L).setTrackingId(DBC_TRACKING_ID + 2).setData(AddiDiffGeneratorTest.XML_CONTENT).setStatus(ChunkItem.Status.IGNORE).build());
        final List<ChunkItem> nextItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setData(AddiDiffGeneratorTest.XML_CONTENT).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(1L).setData(AddiDiffGeneratorTest.XML_CONTENT).setStatus(ChunkItem.Status.SUCCESS).build());
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setItems(currentItems)
                .setNextItems(nextItems)
                .build();

        final Chunk result = getDiffMessageProcessorBean().processPayload(chunk);
        assertThat("number of chunk items in result", result.size(), is(currentItems.size()));

        final Iterator<ChunkItem> iterator = result.iterator();

        ChunkItem item = iterator.next();
        assertThat("1st item status", item.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("1st item diagnostics", item.getDiagnostics().size(), is(1));
        assertThat("1st item trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 1));

        item = iterator.next();
        assertThat("2nd item status", item.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("2nd item diagnostics", item.getDiagnostics().size(), is(1));
        assertThat("2nd item trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 2));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void diffExpectedFailures() throws SinkException {
        final ChunkItem currentItem = new ChunkItemBuilder().setId(0L)
            .setTrackingId(DBC_TRACKING_ID + 1)
            .setDiagnostics(Collections.singletonList(new Diagnostic(
                Diagnostic.Level.FATAL, "expected failure")
                .withTag(FailRecord.class.getName())))
            .build();
        final ChunkItem nextItem = new ChunkItemBuilder().setId(0L)
            .setDiagnostics(Collections.singletonList(new Diagnostic(
                Diagnostic.Level.FATAL, "expected failure")
                .withTag(FailRecord.class.getName())))
            .build();
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
            .setItems(Collections.singletonList(currentItem))
            .setNextItems(Collections.singletonList(nextItem))
            .build();

        final Chunk result = getDiffMessageProcessorBean().processPayload(chunk);
        assertThat("number of chunk items", result.size(), is(1));

        final ChunkItem item = result.iterator().next();
        assertThat("status", item.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("diagnostics", item.getDiagnostics(), is(nullValue()));
        assertThat("trackingId", item.getTrackingId(), is(DBC_TRACKING_ID + 1));
    }

    private DiffMessageProcessorBean getDiffMessageProcessorBean() {
        final DiffMessageProcessorBean diffMessageProcessorBean = new DiffMessageProcessorBean();
        diffMessageProcessorBean.externalToolDiffGenerator = newXmlDiffGenerator();
        diffMessageProcessorBean.addiDiffGenerator = new AddiDiffGenerator();
        diffMessageProcessorBean.addiDiffGenerator.externalToolDiffGenerator =
            diffMessageProcessorBean.externalToolDiffGenerator;
        diffMessageProcessorBean.jobStoreServiceConnectorBean = jobStoreServiceConnectorBean;
        return diffMessageProcessorBean;
    }
}
