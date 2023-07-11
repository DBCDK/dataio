package dk.dbc.dataio.sink.ims;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ImsSinkConfig;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.ims.connector.ImsServiceConnectorTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ImsMessageConsumerTest {
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final ImsConfig imsConfig = mock(ImsConfig.class);
    private final ImsMessageConsumer imsMessageConsumer = new ImsMessageConsumer(new ServiceHub.Builder().withJobStoreServiceConnector(jobStoreServiceConnector).test(), imsConfig);
    private final JSONBContext jsonbContext = new JSONBContext();
    @Rule  // Port 0 lets wiremock find a random port
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Before
    public void setupMocks() {
        when(imsConfig.getConfig(any(ConsumedMessage.class))).thenReturn(new ImsSinkConfig().withEndpoint(getWireMockEndpoint()));
    }

    @Test
    public void handleConsumedMessage() throws InvalidMessageException {
        ImsServiceConnectorTest.MarcXchangeRecordsTwoOkOneFail requestResponse = new ImsServiceConnectorTest.MarcXchangeRecordsTwoOkOneFail();
        requestResponse.stub();
        Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).setJobId(42).setChunkId(0).setItems(requestResponse.getChunkItemsForRequest()).build();
        imsMessageConsumer.handleConsumedMessage(getConsumedMessageForChunk(chunk));
    }

    private ConsumedMessage getConsumedMessageForChunk(Chunk chunk) {
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
            return new ConsumedMessage("messageId", headers, jsonbContext.marshall(chunk));
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    private Chunk getIgnoredChunk() {
        return new ChunkBuilder(Chunk.Type.PROCESSED).setItems(Collections.singletonList(
                        new ChunkItemBuilder().setStatus(ChunkItem.Status.IGNORE).build()))
                .build();
    }

    private String getWireMockEndpoint() {
        return String.format("http://localhost:%d/", wireMockRule.port());
    }
}
