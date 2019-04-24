/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.vip;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.VipSinkConfig;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.sink.types.SinkException;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// TODO: 24-04-19 Add wiremock request/response tests when VIP-CORE service is available 

public class MessageConsumerBeanTest {
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final JobStoreServiceConnectorBean jobStoreServiceConnectorBean = mock(JobStoreServiceConnectorBean.class);
    private final ConfigBean configBean = mock(ConfigBean.class);

    private final MessageConsumerBean messageConsumerBean = new MessageConsumerBean();
    {
        messageConsumerBean.jobStoreServiceConnectorBean = jobStoreServiceConnectorBean;
        messageConsumerBean.configBean = configBean;
    }

    @Before
    public void setupMocks() throws SinkException {
        when(jobStoreServiceConnectorBean.getConnector()).thenReturn(jobStoreServiceConnector);
        when(configBean.getConfig(any(ConsumedMessage.class))).thenReturn(new VipSinkConfig()
                .withEndpoint("endpoint"));
    }

    @Test
    public void handleChunk() {
        final List<ChunkItem> chunkItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L)  // failed by job processor
                        .setStatus(ChunkItem.Status.FAILURE).build(),
                new ChunkItemBuilder().setId(1L)  // ignored by job processor
                        .setStatus(ChunkItem.Status.IGNORE).build(),
                new ChunkItemBuilder().setId(2L)  // invalid addi metadata
                        .setStatus(ChunkItem.Status.SUCCESS)
                        .setData(createAddiRecord("not JSON", "{}").getBytes())
                        .build()
        );
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setChunkId(0L)
                .setItems(chunkItems)
                .build();

        final Chunk result = messageConsumerBean.handleChunk(chunk);

        assertThat("number of chunk items", result.size(), is(3));
        assertThat("1st chunk item",
                result.getItems().get(0).getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("2nd chunk item",
                result.getItems().get(1).getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("3rd chunk item",
                result.getItems().get(2).getStatus(), is(ChunkItem.Status.FAILURE));
    }

    private static AddiRecord createAddiRecord(String metadata, String content) {
        return new AddiRecord(StringUtil.asBytes(metadata), StringUtil.asBytes(content));
    }
}