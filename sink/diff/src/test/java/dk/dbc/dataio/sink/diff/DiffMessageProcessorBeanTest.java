package dk.dbc.dataio.sink.diff;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DiffMessageProcessorBeanTest {
    private final JobStoreServiceConnectorBean jobStoreServiceConnectorBean = mock(JobStoreServiceConnectorBean.class);
    private JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);

    @Before
    public void setupMocks() {
        when(jobStoreServiceConnectorBean.getConnector()).thenReturn(jobStoreServiceConnector);
    }

    @Test
    public void handleConsumedMessage_onValidInputMessage_newOutputMessageEnqueued() throws ServiceException, InvalidMessageException, JsonException, JobStoreServiceConnectorException {
        final String messageId = "id";
        final String payloadType = JmsConstants.CHUNK_PAYLOAD_TYPE;
        final ExternalChunk processedChunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setJobId(0L).setChunkId(0L).build();
        final String payload = JsonUtil.toJson(processedChunk);
        final ConsumedMessage consumedMessage = new ConsumedMessage(messageId, payloadType, payload);
        getDiffMessageProcessorBean().handleConsumedMessage(consumedMessage);

        verify(jobStoreServiceConnector).addChunkIgnoreDuplicates(any(ExternalChunk.class), anyLong(), anyLong());
    }


    @Test
    public void failOnMissingNextItems() throws Exception {
        final List<ChunkItem> processedChunkItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.FAILURE).build(),
                new ChunkItemBuilder().setId(1L).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setStatus(ChunkItem.Status.IGNORE).build()
        );
        final ExternalChunk chunkResult = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED)
                .setItems(processedChunkItems)
                .build();
        final ExternalChunk deliveredChunk = getDiffMessageProcessorBean().processPayload(chunkResult);
        assertThat(deliveredChunk.size(), is(processedChunkItems.size()));
        Iterator<ChunkItem> iterator = deliveredChunk.iterator();
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item0 = iterator.next();
        assertThat(item0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item1 = iterator.next();
        assertThat(item1.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item2 = iterator.next();
        assertThat(item2.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(iterator.hasNext(), is(false));

    }

    @Test
    public void processPayload_FailDifferentContent() {
        final List<ChunkItem> processedChunkItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setData(getXml()).setStatus(ChunkItem.Status.FAILURE).build(),
                new ChunkItemBuilder().setId(1L).setData(getXml()).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setData(getXml()).setStatus(ChunkItem.Status.IGNORE).build());
        final List<ChunkItem> processedChunkNextItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setData(getXmlNext()).setStatus(ChunkItem.Status.FAILURE).build(),
                new ChunkItemBuilder().setId(1L).setData(getXmlNext()).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setData(getXml()).setStatus(ChunkItem.Status.IGNORE).build());

        final ExternalChunk chunkResult = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED)
                .setItems(processedChunkItems)
                .setNextItems(processedChunkNextItems)
                .build();

        final ExternalChunk deliveredChunk = getDiffMessageProcessorBean().processPayload(chunkResult);
        assertThat(deliveredChunk.size(), is(processedChunkItems.size()));
        Iterator<ChunkItem> iterator = deliveredChunk.iterator();
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item0 = iterator.next();
        assertThat(item0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item1 = iterator.next();
        assertThat(item1.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item2 = iterator.next();
        assertThat(item2.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void processPayload_FailDifferentOrInvalidAddiContent() throws IOException {
        final byte[] addi1 = getAddi(getMeta("current"), getContent("current title"));
        final byte[] addi2 = getAddi(getMeta("next"), getContent("current title"));
        final byte[] addi3 = getAddi(getMeta("current"), getContent("next title"));
        final byte[] addi4 = getAddi(getMeta("next"), getContent("next title"));
        final byte[] addi5 = getAddi(getMeta("current"), getInvalidContent());

        final List<ChunkItem> processedChunkItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setData(addi1).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(1L).setData(addi1).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setData(addi1).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(3L).setData(addi1).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(4L).setData(addi1).setStatus(ChunkItem.Status.SUCCESS).build());

        final List<ChunkItem> processedChunkNextItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setData(addi1).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(1L).setData(addi2).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setData(addi3).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(3L).setData(addi4).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(4L).setData(addi5).setStatus(ChunkItem.Status.SUCCESS).build());

        final ExternalChunk chunkResult = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED)
                .setItems(processedChunkItems)
                .setNextItems(processedChunkNextItems)
                .build();

        final ExternalChunk deliveredChunk = getDiffMessageProcessorBean().processPayload(chunkResult);
        assertThat(deliveredChunk.size(), is(processedChunkItems.size()));
        Iterator<ChunkItem> iterator = deliveredChunk.iterator();
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item0 = iterator.next();
        assertThat(item0.getStatus(), is(ChunkItem.Status.SUCCESS));
        ChunkItem item1 = iterator.next();
        assertThat(item1.getStatus(), is(ChunkItem.Status.FAILURE));
        ChunkItem item2 = iterator.next();
        assertThat(item2.getStatus(), is(ChunkItem.Status.FAILURE));
        ChunkItem item3 = iterator.next();
        assertThat(item3.getStatus(), is(ChunkItem.Status.FAILURE));
        ChunkItem item4 = iterator.next();
        assertThat(item4.getStatus(), is(ChunkItem.Status.FAILURE));
    }

    @Test
    public void processPayload_FailDifferentStatus() {
        final List<ChunkItem> processedChunkItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setData(getXml()).setStatus(ChunkItem.Status.FAILURE).build(),
                new ChunkItemBuilder().setId(1L).setData(getXml()).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setData(getXml()).setStatus(ChunkItem.Status.IGNORE).build());
        final List<ChunkItem> processedChunkNextItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setData(getXmlNext()).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(1L).setData(getXml()).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setData(getXmlNext()).setStatus(ChunkItem.Status.SUCCESS).build());

        final ExternalChunk chunkResult = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED)
                .setItems(processedChunkItems)
                .setNextItems(processedChunkNextItems)
                .build();

        final ExternalChunk deliveredChunk = getDiffMessageProcessorBean().processPayload(chunkResult);
        assertThat(deliveredChunk.size(), is(processedChunkItems.size()));
        Iterator<ChunkItem> iterator = deliveredChunk.iterator();
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item0 = iterator.next();
        assertThat(item0.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item1 = iterator.next();
        assertThat(item1.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item2 = iterator.next();
        assertThat(item2.getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat(iterator.hasNext(), is(false));
    }


    /* Private methods */

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
        return (metaXml.trim().getBytes().length +
                System.lineSeparator() +
                metaXml +
                System.lineSeparator() +
                contentXml.trim().getBytes().length +
                System.lineSeparator() +
                contentXml).getBytes();
    }

}
