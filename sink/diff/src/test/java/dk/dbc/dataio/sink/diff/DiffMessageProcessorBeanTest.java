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

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.sink.types.SinkException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DiffMessageProcessorBeanTest {
    private final JobStoreServiceConnectorBean jobStoreServiceConnectorBean = mock(JobStoreServiceConnectorBean.class);
    private JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final JSONBContext jsonbContext = new JSONBContext();
    private final static String DBC_TRACKING_ID = "dataio_";

    @Before
    public void setupMocks() {
        when(jobStoreServiceConnectorBean.getConnector()).thenReturn(jobStoreServiceConnector);
    }

    @Test
    public void handleConsumedMessage_onValidInputMessage_newOutputMessageEnqueued() throws ServiceException, InvalidMessageException, JobStoreServiceConnectorException, JSONBException {
        final String messageId = "id";
        final Map<String, Object> headers = Collections.singletonMap(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
        final Chunk processedChunk = new ChunkBuilder(Chunk.Type.PROCESSED).setJobId(0L).setChunkId(0L).build();
        final String payload = jsonbContext.marshall(processedChunk);
        final ConsumedMessage consumedMessage = new ConsumedMessage(messageId, headers, payload);

        getDiffMessageProcessorBean().handleConsumedMessage(consumedMessage);

        verify(jobStoreServiceConnector).addChunkIgnoreDuplicates(any(Chunk.class), anyLong(), anyLong());
    }


    @Test
    public void failOnMissingNextItems() throws Exception {
        final List<ChunkItem> processedChunkItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setTrackingId(DBC_TRACKING_ID + 1).setStatus(ChunkItem.Status.FAILURE).build(),
                new ChunkItemBuilder().setId(1L).setTrackingId(DBC_TRACKING_ID + 2).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setTrackingId(DBC_TRACKING_ID + 3).setStatus(ChunkItem.Status.IGNORE).build()
        );
        final Chunk chunkResult = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setItems(processedChunkItems)
                .build();
        final Chunk deliveredChunk = getDiffMessageProcessorBean().processPayload(chunkResult);
        assertThat(deliveredChunk.size(), is(processedChunkItems.size()));
        Iterator<ChunkItem> iterator = deliveredChunk.iterator();
        assertThat(iterator.hasNext(), is(true));

        ChunkItem item0 = iterator.next();
        assertThat("ChunkItem0.getStatus()", item0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("ChunkItem0.getDiagnostics", item0.getDiagnostics().size(), is(1));
        assertThat("ChunkItem0.getDiagnostics.stacktrace", item0.getDiagnostics().get(0).getStacktrace(), is(nullValue()));
        assertThat("ChunkItem0.trackingId", item0.getTrackingId(), is(DBC_TRACKING_ID+ 1));
        assertThat(iterator.hasNext(), is(true));

        ChunkItem item1 = iterator.next();
        assertThat("ChunkItem1.getStatus()", item1.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("ChunkItem1.getDiagnostics", item1.getDiagnostics().size(), is(1));
        assertThat("ChunkItem1.getDiagnostics.stacktrace", item1.getDiagnostics().get(0).getStacktrace(), is(nullValue()));
        assertThat("ChunkItem1.trackingId", item1.getTrackingId(), is(DBC_TRACKING_ID + 2));
        assertThat(iterator.hasNext(), is(true));

        ChunkItem item2 = iterator.next();
        assertThat("ChunkItem2.getStatus()", item2.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("ChunkItem2.getDiagnostics", item2.getDiagnostics().size(), is(1));
        assertThat("ChunkItem2.getDiagnostics.stacktrace", item2.getDiagnostics().get(0).getStacktrace(), is(nullValue()));
        assertThat("ChunkItem2.trackingId", item2.getTrackingId(), is(DBC_TRACKING_ID + 3));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void processPayload_FailDifferentContent() throws SinkException {
        final List<ChunkItem> processedChunkItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setTrackingId(DBC_TRACKING_ID + 1).setData(getXml()).setStatus(ChunkItem.Status.FAILURE).build(),
                new ChunkItemBuilder().setId(1L).setTrackingId(DBC_TRACKING_ID + 2).setData(getXml()).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setTrackingId(DBC_TRACKING_ID + 3).setData(getXml()).setStatus(ChunkItem.Status.IGNORE).build(),
                new ChunkItemBuilder().setId(3L).setTrackingId(DBC_TRACKING_ID + 4).setData(getXml()).setStatus(ChunkItem.Status.SUCCESS).build());
        final List<ChunkItem> processedChunkNextItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setData(getXmlNext()).setStatus(ChunkItem.Status.FAILURE).build(),
                new ChunkItemBuilder().setId(1L).setData(getXmlNext()).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setData(getXml()).setStatus(ChunkItem.Status.IGNORE).build(),
                new ChunkItemBuilder().setId(3L).setData(getXml()).setStatus(ChunkItem.Status.SUCCESS).build());

        final Chunk chunkResult = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setItems(processedChunkItems)
                .setNextItems(processedChunkNextItems)
                .build();

        final Chunk deliveredChunk = getDiffMessageProcessorBean().processPayload(chunkResult);
        assertThat(deliveredChunk.size(), is(processedChunkItems.size()));
        Iterator<ChunkItem> iterator = deliveredChunk.iterator();
        assertThat(iterator.hasNext(), is(true));

        ChunkItem item0 = iterator.next();
        assertThat("ChunkItem0.getStatus()", item0.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("ChunkItem0.getDiagnostics", item0.getDiagnostics(), is(nullValue()));
        assertThat("ChunkItem0.getTrackingId()", item0.getTrackingId(), is(DBC_TRACKING_ID + 1));
        assertThat(iterator.hasNext(), is(true));

        ChunkItem item1 = iterator.next();
        assertThat("ChunkItem1.getStatus()", item1.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("ChunkItem1.getDiagnostics", item1.getDiagnostics().size(), is(1));
        assertThat("ChunkItem1.getDiagnostics.stacktrace", item1.getDiagnostics().get(0).getStacktrace(), is(nullValue()));
        assertThat("ChunkItem1.getTrackingId()", item1.getTrackingId(), is(DBC_TRACKING_ID + 2));
        assertThat(iterator.hasNext(), is(true));

        ChunkItem item2 = iterator.next();
        assertThat("ChunkItem2.getStatus()", item2.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("ChunkItem2.getDiagnostics", item2.getDiagnostics(), is(nullValue()));
        assertThat("ChunkItem2.getTrackingId()", item2.getTrackingId(), is(DBC_TRACKING_ID + 3));
        assertThat(iterator.hasNext(), is(true));

        ChunkItem item3 = iterator.next();
        assertThat("ChunkItem3.getStatus()", item3.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("ChunkItem3.getDiagnostics", item3.getDiagnostics(), is(nullValue()));
        assertThat("ChunkItem3.getTrackingId()", item3.getTrackingId(), is(DBC_TRACKING_ID + 4));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void processPayload_FailDifferentOrInvalidAddiContent() throws IOException, SinkException {
        final byte[] addi1 = getAddi(getMeta("current"), getContent("current title"));
        final byte[] addi2 = getAddi(getMeta("next"), getContent("current title"));
        final byte[] addi3 = getAddi(getMeta("current"), getContent("next title"));
        final byte[] addi4 = getAddi(getMeta("next"), getContent("next title"));
        final byte[] addi5 = getAddi(getMeta("current"), getInvalidContent());

        final List<ChunkItem> processedChunkItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setTrackingId(DBC_TRACKING_ID + 1).setData(addi1).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(1L).setTrackingId(DBC_TRACKING_ID + 2).setData(addi1).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setTrackingId(DBC_TRACKING_ID + 3).setData(addi1).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(3L).setTrackingId(DBC_TRACKING_ID + 4).setData(addi1).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(4L).setTrackingId(DBC_TRACKING_ID + 5).setData(addi1).setStatus(ChunkItem.Status.SUCCESS).build());

        final List<ChunkItem> processedChunkNextItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setData(addi1).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(1L).setData(addi2).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setData(addi3).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(3L).setData(addi4).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(4L).setData(addi5).setStatus(ChunkItem.Status.SUCCESS).build());

        final Chunk chunkResult = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setItems(processedChunkItems)
                .setNextItems(processedChunkNextItems)
                .build();

        final Chunk deliveredChunk = getDiffMessageProcessorBean().processPayload(chunkResult);
        assertThat(deliveredChunk.size(), is(processedChunkItems.size()));
        Iterator<ChunkItem> iterator = deliveredChunk.iterator();
        assertThat(iterator.hasNext(), is(true));

        ChunkItem item0 = iterator.next();
        assertThat("ChunkItem0.getStatus()", item0.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("ChunkItem0.getDiagnostics", item0.getDiagnostics(), is(nullValue()));
        assertThat("ChunkItem0.getTrackingId()", item0.getTrackingId(), is(DBC_TRACKING_ID + 1));
        assertThat(iterator.hasNext(), is(true));

        ChunkItem item1 = iterator.next();
        assertThat("ChunkItem1.getStatus()", item1.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("ChunkItem1.getDiagnostics", item1.getDiagnostics().size(), is(1));
        assertThat("ChunkItem1.getDiagnostics.stacktrace", item1.getDiagnostics().get(0).getStacktrace(), is(nullValue()));
        assertThat("ChunkItem1.getTrackingId()", item1.getTrackingId(), is(DBC_TRACKING_ID + 2));
        assertThat(iterator.hasNext(), is(true));

        ChunkItem item2 = iterator.next();
        assertThat("ChunkItem2.getStatus()", item2.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("ChunkItem2.getDiagnostics", item2.getDiagnostics().size(), is(1));
        assertThat("ChunkItem2.getDiagnostics.stacktrace", item2.getDiagnostics().get(0).getStacktrace(), is(nullValue()));
        assertThat("ChunkItem2.getTrackingId()", item2.getTrackingId(), is(DBC_TRACKING_ID + 3));
        assertThat(iterator.hasNext(), is(true));

        ChunkItem item3 = iterator.next();
        assertThat("ChunkItem3.getStatus()", item3.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("ChunkItem3.getDiagnostics", item3.getDiagnostics().size(), is(1));
        assertThat("ChunkItem3.getDiagnostics.stacktrace", item3.getDiagnostics().get(0).getStacktrace(), is(nullValue()));
        assertThat("ChunkItem3.getTrackingId()", item3.getTrackingId(), is(DBC_TRACKING_ID + 4));
        assertThat(iterator.hasNext(), is(true));

        ChunkItem item4 = iterator.next();
        assertThat("ChunkItem4.getStatus()", item4.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("ChunkItem4.getDiagnostics", item4.getDiagnostics().size(), is(1));
        assertThat("ChunkItem4.getDiagnostics.stacktrace", item4.getDiagnostics().get(0).getStacktrace(), is(notNullValue()));
        assertThat("ChunkItem4.getTrackingId()", item4.getTrackingId(), is(DBC_TRACKING_ID + 5));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void processPayload_FailDifferentStatus() throws SinkException {
        final List<ChunkItem> processedChunkItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setTrackingId(DBC_TRACKING_ID + 1).setData(getXml()).setStatus(ChunkItem.Status.FAILURE).build(),
                new ChunkItemBuilder().setId(1L).setTrackingId(DBC_TRACKING_ID + 2).setData(getXml()).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setTrackingId(DBC_TRACKING_ID + 3).setData(getXml()).setStatus(ChunkItem.Status.IGNORE).build());
        final List<ChunkItem> processedChunkNextItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setData(getXmlNext()).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(1L).setData(getXml()).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setData(getXmlNext()).setStatus(ChunkItem.Status.SUCCESS).build());

        final Chunk chunkResult = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setItems(processedChunkItems)
                .setNextItems(processedChunkNextItems)
                .build();

        final Chunk deliveredChunk = getDiffMessageProcessorBean().processPayload(chunkResult);
        assertThat(deliveredChunk.size(), is(processedChunkItems.size()));
        Iterator<ChunkItem> iterator = deliveredChunk.iterator();
        assertThat(iterator.hasNext(), is(true));

        ChunkItem item0 = iterator.next();
        assertThat("ChunkItem0.getStatus()", item0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("ChunkItem0.getDiagnostics", item0.getDiagnostics().size(), is(1));
        assertThat("ChunkItem0.getDiagnostics.stacktrace", item0.getDiagnostics().get(0).getStacktrace(), is(nullValue()));
        assertThat("ChunkItem0.getTrackingId()", item0.getTrackingId(), is(DBC_TRACKING_ID + 1));
        assertThat(iterator.hasNext(), is(true));

        ChunkItem item1 = iterator.next();
        assertThat("ChunkItem1.getStatus()", item1.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("ChunkItem1.getDiagnostics", item1.getDiagnostics(), is(nullValue()));
        assertThat("ChunkItem1.getTrackingId()", item1.getTrackingId(), is(DBC_TRACKING_ID + 2));
        assertThat(iterator.hasNext(), is(true));

        ChunkItem item2 = iterator.next();
        assertThat("ChunkItem2.getStatus()", item2.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("ChunkItem2.getDiagnostics", item2.getDiagnostics().size(), is(1));
        assertThat("ChunkItem2.getDiagnostics.stacktrace", item2.getDiagnostics().get(0).getStacktrace(), is(nullValue()));
        assertThat("ChunkItem2.getTrackingId()", item2.getTrackingId(), is(DBC_TRACKING_ID + 3));
        assertThat(iterator.hasNext(), is(false));
    }

    private DiffMessageProcessorBean getDiffMessageProcessorBean() {
        final DiffMessageProcessorBean diffMessageProcessorBean = new DiffMessageProcessorBean();
        diffMessageProcessorBean.jobStoreServiceConnectorBean = jobStoreServiceConnectorBean;
        return diffMessageProcessorBean;
    }

    private byte[] getXml() {
        return ("<?xml version='1.0'?><dataio-harvester-datafile><data-container>" +
                "<data><collection xmlns=\"info:lc/xmlns/marcxchange-v1\">" +
                "<record xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd\">" +
                "<leader>00000n 2200000 4500</leader>" +
                "<datafield ind1=\"0\" ind2=\"0\" tag=\"001\">" +
                "<subfield code=\"a\">Sun Kil Moon</subfield>" +
                "</datafield></record></collection></data></data-container></dataio-harvester-datafile>").getBytes();
    }

    private byte[] getXmlNext() {
        return ("<dataio-harvester-datafile>" +
                "<data-container>" +
                "<data><collection xmlns=\"info:lc/xmlns/marcxchange-v1\">" +
                "<record xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd\">" +
                "<leader>00000n 2200000 4500</leader> " +
                "<datafield ind1=\"0\" ind2=\"0\" tag=\"001\">" +
                "<subfield code=\"a\">Sun Kil Sun</subfield>" +
                "</datafield></record></collection></data></data-container></dataio-harvester-datafile>").getBytes();
    }

    private String getMeta(String attributeValue) {
        return "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
                "<es:info format=\"" + attributeValue + "\" language=\"dan\" submitter=\"870970\"/>" +
                "<dataio:sink-processing xmlns:dataio=\"dk.dbc.dataio.processing\" encodeAs2709=\"true\" charset=\"danmarc2\"/>" +
                "</es:referencedata>";
    }

    private String getContent(String contentAttributeValue) {
        return "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                "<marcx:record format=\"danMARC2\">" +
                "<marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                "<marcx:subfield code=\"a\">" + contentAttributeValue + "</marcx:subfield>" +
                "</marcx:datafield></marcx:record></marcx:collection>";
    }

    private String getInvalidContent() {
        return  "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                "<marcx:record format=\"danMARC2\">" +
                "<marcx:subfield code=\"a\">title1</marcx:subfield>" +
                "</marcx:datafield></marcx:record></marcx:collection>";
    }

    private byte[] getAddi(String metaXml, String contentXml) {
        return new AddiRecord(
                metaXml.trim().getBytes(StandardCharsets.UTF_8),
                contentXml.trim().getBytes(StandardCharsets.UTF_8)).getBytes();
    }

}
