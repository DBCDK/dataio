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

package dk.dbc.dataio.integrationtest;

import dk.dbc.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class JmsQueueConnector {

    public static final String JMS_QUEUE_SERVICE_BASEURL = String.format("http://%s:%s/jms-queue-service",
            System.getProperty("container.hostname"), System.getProperty("container.http.port"));
    public static final String QUEUE_RESOURCE_ENDPOINT = "queue";
    public static final String SINKS_QUEUE_NAME = "jms/dataio/sinks";
    public static final String PROCESSOR_QUEUE_NAME = "jms/dataio/processor";

    private static final Client REST_CLIENT = HttpClient.newClient(new ClientConfig().register(new JacksonFeature()));
    private static final String ENCODING = StandardCharsets.UTF_8.name();
    private static final long SLEEP_INTERVAL_IN_MS = 250;

    private JmsQueueConnector() {
    }

    public static List<MockedJmsTextMessage> listQueue(String queueName) {
        final Response response;
        try {
            response = HttpClient.doGet(REST_CLIENT, JMS_QUEUE_SERVICE_BASEURL, QUEUE_RESOURCE_ENDPOINT,
                    URLEncoder.encode(queueName, ENCODING));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
        assertOkStatusCode(response);
        return response.readEntity(new GenericType<List<MockedJmsTextMessage>>() {
        });
    }

    public static void putOnQueue(String queueName, MockedJmsTextMessage message) {
        final Response response;
        try {
            response = HttpClient.doPostWithJson(REST_CLIENT, message, JMS_QUEUE_SERVICE_BASEURL, QUEUE_RESOURCE_ENDPOINT,
                    URLEncoder.encode(queueName, ENCODING));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
        assertOkStatusCode(response);
    }

    public static int emptyQueue(String queueName) {
        final Response response;
        try {
            response = HttpClient.doDelete(REST_CLIENT, JMS_QUEUE_SERVICE_BASEURL, QUEUE_RESOURCE_ENDPOINT,
                    URLEncoder.encode(queueName, ENCODING));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
        assertOkStatusCode(response);
        return response.readEntity(Integer.class);
    }

    public static int getQueueSize(String queueName) {
        final Response response;
        try {
            response = HttpClient.doGet(REST_CLIENT, JMS_QUEUE_SERVICE_BASEURL, QUEUE_RESOURCE_ENDPOINT,
                    URLEncoder.encode(queueName, ENCODING), "size");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
        assertOkStatusCode(response);
        return response.readEntity(Integer.class);
    }

    public static void awaitQueueSize(String queueName, int expectedQueueSize, long maxWaitInMs) {
        long remainingWaitInMs = maxWaitInMs;
        int actualQueueSize = getQueueSize(queueName);
        while (actualQueueSize != expectedQueueSize && remainingWaitInMs > 0) {
            try {
                Thread.sleep(SLEEP_INTERVAL_IN_MS);
                remainingWaitInMs -= SLEEP_INTERVAL_IN_MS;
                actualQueueSize = getQueueSize(queueName);
            } catch (InterruptedException e) {
                break;
            }
        }
        if (actualQueueSize != expectedQueueSize) {
            throw new IllegalStateException(String.format("Expected size %d of queue %s differs from actual size %d",
                    expectedQueueSize, queueName, actualQueueSize));
        }
    }

    public static List<MockedJmsTextMessage> awaitQueueList(String queueName, int expectedQueueSize, long maxWaitInMs) {
        awaitQueueSize(queueName, expectedQueueSize, maxWaitInMs);
        return listQueue(queueName);
    }

    private static void assertOkStatusCode(Response response) {
        assert response.getStatus() == Response.Status.OK.getStatusCode() :
                String.format("Expected status code is %d was %d", Response.Status.OK.getStatusCode(), response.getStatus());
    }
}
