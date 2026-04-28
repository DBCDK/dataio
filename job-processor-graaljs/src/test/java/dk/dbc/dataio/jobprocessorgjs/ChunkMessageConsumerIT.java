package dk.dbc.dataio.jobprocessorgjs;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.jms.JMSHeader;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import jakarta.jms.Connection;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.ws.rs.core.Response;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class ChunkMessageConsumerIT extends ContainerTest {

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    @Test
    void serviceStartsAndAcceptsMessages() throws Exception {
        try (Response response = serviceContainer.httpGet()
                .withPathElements("health", "ready")
                .execute()) {
            assertThat("health ready", response.getStatus(), is(200));
        }

        wireMockServer.stubFor(post(urlEqualTo("/jobs/1/chunks/0/processed"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"jobId\":1}")));

        Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(1)
                .setChunkId(0)
                .setItems(Collections.emptyList())
                .build();

        sendJmsChunk(new ObjectMapper().writeValueAsString(chunk), 1, 0, 42L, 1L, "test-it");

        await().atMost(Duration.ofSeconds(10)).until(() ->
                !wireMockServer.findAll(postRequestedFor(urlEqualTo("/jobs/1/chunks/0/processed"))).isEmpty());
    }

    @Test
    void scriptIsExecutedForChunkWithItems() throws Exception {
        byte[] jsar = buildJsar("function process(data, supplement) { return data.toUpperCase(); }");

        String flowJson = "{\"id\":42,\"version\":1,\"content\":{" +
                "\"name\":\"test-flow\"," +
                "\"description\":\"test\"," +
                "\"entrypointScript\":\"main.js\"," +
                "\"entrypointFunction\":\"process\"," +
                "\"engine\":\"GRAALJS\"}}";

        wireMockServer.stubFor(get(urlEqualTo("/jobs/1/cachedflow"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(flowJson)));
        wireMockServer.stubFor(get(urlEqualTo("/flows/42/jsar"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/octet-stream")
                        .withBody(jsar)));
        wireMockServer.stubFor(post(urlEqualTo("/jobs/1/chunks/2/processed"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"jobId\":1}")));

        Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(1)
                .setChunkId(2)
                .setItems(List.of(
                        new ChunkItemBuilder()
                                .setId(0)
                                .setData("hello")
                                .setStatus(ChunkItem.Status.SUCCESS)
                                .setTrackingId("track-0")
                                .build()
                ))
                .build();

        sendJmsChunk(new ObjectMapper().writeValueAsString(chunk), 1, 2, 42L, 1L, "test-it-script");

        await().atMost(Duration.ofSeconds(30)).until(() ->
                !wireMockServer.findAll(postRequestedFor(urlEqualTo("/jobs/1/chunks/2/processed"))).isEmpty());

        Chunk processed = new ObjectMapper().readValue(
                wireMockServer.findAll(postRequestedFor(urlEqualTo("/jobs/1/chunks/2/processed"))).getFirst().getBodyAsString(),
                Chunk.class);
        assertThat("chunk type", processed.getType(), is(Chunk.Type.PROCESSED));
        assertThat("item count", processed.getItems().size(), is(1));
        ChunkItem item = processed.getItems().getFirst();
        assertThat("item status", item.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("item data", new String(item.getData(), StandardCharsets.UTF_8), is("HELLO"));
    }

    @Test
    void addiMetadataIsPassedToScriptAsSupplement() throws Exception {
        byte[] addiData = new AddiRecord(
                "{\"format\":\"test-format\"}".getBytes(StandardCharsets.UTF_8),
                "hello".getBytes(StandardCharsets.UTF_8)).getBytes();

        byte[] jsar = buildJsar("function process(data, supplement) { return supplement.format + ':' + data; }");

        String flowJson = "{\"id\":42,\"version\":2,\"content\":{" +
                "\"name\":\"test-flow\"," +
                "\"description\":\"test\"," +
                "\"entrypointScript\":\"main.js\"," +
                "\"entrypointFunction\":\"process\"," +
                "\"engine\":\"GRAALJS\"}}";

        wireMockServer.stubFor(get(urlEqualTo("/jobs/1/cachedflow"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(flowJson)));
        wireMockServer.stubFor(get(urlEqualTo("/flows/42/jsar"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/octet-stream")
                        .withBody(jsar)));
        wireMockServer.stubFor(post(urlEqualTo("/jobs/1/chunks/3/processed"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"jobId\":1}")));

        Chunk chunk = new ChunkBuilder(Chunk.Type.PARTITIONED)
                .setJobId(1)
                .setChunkId(3)
                .setItems(List.of(
                        ChunkItem.successfulChunkItem()
                                .withId(0)
                                .withType(ChunkItem.Type.ADDI, ChunkItem.Type.BYTES)
                                .withData(addiData)
                                .withTrackingId("track-addi")
                ))
                .build();

        sendJmsChunk(new ObjectMapper().writeValueAsString(chunk), 1, 3, 42L, 2L, "test-it-addi");

        await().atMost(Duration.ofSeconds(30)).until(() ->
                !wireMockServer.findAll(postRequestedFor(urlEqualTo("/jobs/1/chunks/3/processed"))).isEmpty());

        Chunk processed = new ObjectMapper().readValue(
                wireMockServer.findAll(postRequestedFor(urlEqualTo("/jobs/1/chunks/3/processed"))).getFirst().getBodyAsString(),
                Chunk.class);
        assertThat("chunk type", processed.getType(), is(Chunk.Type.PROCESSED));
        assertThat("item count", processed.getItems().size(), is(1));
        ChunkItem item = processed.getItems().getFirst();
        assertThat("item status", item.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("item data", new String(item.getData(), StandardCharsets.UTF_8), is("test-format:hello"));
    }

    private static void sendJmsChunk(String json, int jobId, int chunkId, long flowId,
                                     long flowVersion, String trackingId) throws Exception {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("tcp://localhost:" + ARTEMIS_PORT);
        try (Connection conn = cf.createConnection()) {
            conn.start();
            Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(
                    session.createQueue(QUEUE_NAME + "::" + QUEUE_NAME));
            TextMessage msg = session.createTextMessage(json);
            JMSHeader.payload.addHeader(msg, JMSHeader.CHUNK_PAYLOAD_TYPE);
            JMSHeader.jobId.addHeader(msg, jobId);
            JMSHeader.chunkId.addHeader(msg, chunkId);
            JMSHeader.trackingId.addHeader(msg, trackingId);
            JMSHeader.flowId.addHeader(msg, flowId);
            JMSHeader.flowVersion.addHeader(msg, flowVersion);
            JMSHeader.additionalArgs.addHeader(msg, "{}");
            producer.send(msg);
        }
    }

    private static byte[] buildJsar(String jsSource) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            addEntry(zos, "META-INF/MANIFEST.MF",
                    """
                            Manifest-Version: 1.0
                            Flow-Name: test-flow
                            Flow-Description: test
                            Flow-Entrypoint-Script: main.js
                            Flow-Entrypoint-Function: process
                            Flow-JavaScript-Engine: GRAALJS
                            """);
            addEntry(zos, "main.js", jsSource);
        }
        return baos.toByteArray();
    }

    private static void addEntry(ZipOutputStream zos, String name, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }
}
