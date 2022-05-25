package dk.dbc.dataio.jms;

import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.HttpDelete;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.invariant.InvariantUtil;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class JmsQueueServiceConnector {
    private static final String QUEUE_RESOURCE_PATH = "queue";
    private static final long SLEEP_INTERVAL_IN_MS = 250;

    public enum Queue {
        PROCESSING("jms/dataio/processor"),
        SINK("jms/dataio/sinks");

        private final String queueName;

        Queue(String queueName) {
            this.queueName = queueName;
        }

        public String getQueueName() {
            return queueName;
        }

        public String getUrlEncodedQueueName() {
            try {
                return URLEncoder.encode(queueName, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private final HttpClient httpClient;
    private final String baseUrl;

    public JmsQueueServiceConnector(Client client, String baseUrl) {
        this.httpClient = HttpClient.create(InvariantUtil.checkNotNullOrThrow(client, "client"));
        this.baseUrl = InvariantUtil.checkNotNullNotEmptyOrThrow(baseUrl, "baseUrl");
    }

    public List<MockedJmsTextMessage> listQueue(Queue queue) {
        try (Response response = new HttpGet(httpClient)
                .withBaseUrl(baseUrl)
                .withPathElements(QUEUE_RESOURCE_PATH, queue.getUrlEncodedQueueName())
                .execute()) {
            verifyResponseStatus(response, Response.Status.OK);
            return response.readEntity(new GenericType<List<MockedJmsTextMessage>>() {});
        }
    }

    public void putOnQueue(Queue queue, MockedJmsTextMessage message) {
        try (Response response = new HttpPost(httpClient)
                .withBaseUrl(baseUrl)
                .withPathElements(QUEUE_RESOURCE_PATH, queue.getUrlEncodedQueueName())
                .withJsonData(message)
                .execute()) {
            verifyResponseStatus(response, Response.Status.OK);
        }
    }

    public int emptyQueue(Queue queue) {
        try (Response response = new HttpDelete(httpClient)
                .withBaseUrl(baseUrl)
                .withPathElements(QUEUE_RESOURCE_PATH, queue.getUrlEncodedQueueName())
                .execute()) {
            verifyResponseStatus(response, Response.Status.OK);
            return response.readEntity(Integer.class);
        }
    }

    public int getQueueSize(Queue queue) {
        try (Response response = new HttpGet(httpClient)
                .withBaseUrl(baseUrl)
                .withPathElements(QUEUE_RESOURCE_PATH, queue.getUrlEncodedQueueName(), "size")
                .execute()) {
            verifyResponseStatus(response, Response.Status.OK);
            return response.readEntity(Integer.class);
        }
    }

    public void awaitQueueSize(Queue queue, int expectedQueueSize, long maxWaitInMs) {
        long remainingWaitInMs = maxWaitInMs;
        int actualQueueSize = getQueueSize(queue);
        while (actualQueueSize != expectedQueueSize && remainingWaitInMs > 0) {
            try {
                Thread.sleep(SLEEP_INTERVAL_IN_MS);
                remainingWaitInMs -= SLEEP_INTERVAL_IN_MS;
                actualQueueSize = getQueueSize(queue);
            } catch (InterruptedException e) {
                break;
            }
        }
        if (actualQueueSize != expectedQueueSize) {
            throw new IllegalStateException(String.format("Expected size %d of queue %s differs from actual size %d",
                    expectedQueueSize, queue.getQueueName(), actualQueueSize));
        }
    }

    public List<MockedJmsTextMessage> awaitQueueSizeAndList(
            Queue queue, int expectedQueueSize, long maxWaitInMs) {
        awaitQueueSize(queue, expectedQueueSize, maxWaitInMs);
        return listQueue(queue);
    }

    private void verifyResponseStatus(Response response, Response.Status expectedStatus) {
        if (response.getStatus() != expectedStatus.getStatusCode()) {
            throw new IllegalStateException(
                    String.format("JMS queue service returned with unexpected status code %d expected %s",
                            response.getStatus(), expectedStatus.getStatusCode()));
        }
    }
}
