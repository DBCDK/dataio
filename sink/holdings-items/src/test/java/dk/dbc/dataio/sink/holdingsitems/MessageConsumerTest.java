package dk.dbc.dataio.sink.holdingsitems;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.solrdocstore.connector.SolrDocStoreConnector;
import dk.dbc.solrdocstore.connector.SolrDocStoreConnectorException;
import dk.dbc.solrdocstore.connector.SolrDocStoreConnectorUnexpectedStatusCodeException;
import dk.dbc.solrdocstore.connector.model.HoldingsItems;
import dk.dbc.solrdocstore.connector.model.IndexKeys;
import dk.dbc.solrdocstore.connector.model.Status;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageConsumerTest {
    private final SolrDocStoreConnector solrDocStoreConnector = mock(SolrDocStoreConnector.class);
    private final MessageConsumer messageConsumer = newMessageConsumerBean();
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    void handleChunk() throws JSONBException, SolrDocStoreConnectorException {
        IndexKeys indexKeys = new IndexKeys();
        indexKeys.put("k1", Collections.singletonList("v1"));
        List<IndexKeys> indexKeysList = new ArrayList<>();
        indexKeysList.add(indexKeys);

        HoldingsItems holdingsItemsOK1 = new HoldingsItems();
        holdingsItemsOK1.setAgencyId(123456);
        holdingsItemsOK1.setBibliographicRecordId("id1__1");
        holdingsItemsOK1.setIndexKeys(indexKeysList);
        HoldingsItems holdingsItemsFail = new HoldingsItems();
        holdingsItemsFail.setAgencyId(123456);
        holdingsItemsFail.setBibliographicRecordId("id1__2");
        holdingsItemsFail.setIndexKeys(indexKeysList);
        HoldingsItems holdingsItemsOK2 = new HoldingsItems();
        holdingsItemsOK2.setAgencyId(123456);
        holdingsItemsOK2.setBibliographicRecordId("id1__3");
        holdingsItemsOK2.setIndexKeys(indexKeysList);

        Status statusOk = new Status();
        statusOk.setOk(true);
        statusOk.setText("ok");
        when(solrDocStoreConnector.setHoldings(eq(holdingsItemsOK1))).thenReturn(statusOk);
        when(solrDocStoreConnector.setHoldings(eq(holdingsItemsOK2))).thenReturn(statusOk);
        Status statusFail = new Status();
        statusFail.setOk(false);
        statusFail.setText("error");
        SolrDocStoreConnectorUnexpectedStatusCodeException internalServerError =
                new SolrDocStoreConnectorUnexpectedStatusCodeException("internal server error", 500);
        internalServerError.setStatus(statusFail);
        when(solrDocStoreConnector.setHoldings(eq(holdingsItemsFail))).thenThrow(internalServerError);

        AddiRecord addiRecordInvalid = new AddiRecord(new byte[0],
                StringUtil.asBytes("not json"));
        AddiRecord addiRecordFail = new AddiRecord(new byte[0],
                StringUtil.asBytes(jsonbContext.marshall(Arrays.asList(holdingsItemsOK1, holdingsItemsFail, holdingsItemsOK2))));
        AddiRecord addiRecordOk = new AddiRecord(new byte[0],
                StringUtil.asBytes(jsonbContext.marshall(Collections.singletonList(holdingsItemsOK1))));

        List<ChunkItem> chunkItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.FAILURE)
                        .build(),
                new ChunkItemBuilder().setId(1L).setStatus(ChunkItem.Status.SUCCESS)
                        .setData(addiRecordInvalid.getBytes())
                        .build(),
                new ChunkItemBuilder().setId(2L).setStatus(ChunkItem.Status.IGNORE)
                        .build(),
                new ChunkItemBuilder().setId(3L).setStatus(ChunkItem.Status.SUCCESS)
                        .setData(addiRecordOk.getBytes())
                        .build(),
                new ChunkItemBuilder().setId(4L).setStatus(ChunkItem.Status.SUCCESS)
                        .setData(addiRecordFail.getBytes())
                        .build());

        final int jobId = 42;
        Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setJobId(jobId)
                .setChunkId(0L)
                .setItems(chunkItems)
                .build();

        Chunk result = messageConsumer.handleChunk(chunk);

        assertThat("number of chunk items", result.size(), is(5));
        assertThat("1st chunk item",
                result.getItems().get(0).getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("2nd chunk item",
                result.getItems().get(1).getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("3rd chunk item",
                result.getItems().get(2).getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("4th chunk item",
                result.getItems().get(3).getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("5th chunk item",
                result.getItems().get(4).getStatus(), is(ChunkItem.Status.FAILURE));

        assertThat("4th chunk item data", StringUtil.asString(result.getItems().get(3).getData()),
                is("id1__1:123456 consumer service response - ok\n"));
        assertThat("5th chunk item data", StringUtil.asString(result.getItems().get(4).getData()),
                is("id1__1:123456 consumer service response - ok\nid1__2:123456 consumer service response - error\nid1__3:123456 consumer service response - ok\n"));
    }

    private MessageConsumer newMessageConsumerBean() {
        HoldingsItemsUnmarshaller holdingsItemsUnmarshaller = new HoldingsItemsUnmarshaller(solrDocStoreConnector);
        ServiceHub hub = new ServiceHub.Builder().withJobStoreServiceConnector(mock(JobStoreServiceConnector.class)).build();
        return new MessageConsumer(hub, solrDocStoreConnector, holdingsItemsUnmarshaller);
    }
}
