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

package dk.dbc.dataio.gatekeeper;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import org.apache.http.client.config.RequestConfig;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;

public class ConnectorFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorFactory.class);
    private final Client client;
    private final FileStoreServiceConnector fileStoreServiceConnector;
    private final JobStoreServiceConnector jobStoreServiceConnector;
    private final FlowStoreServiceConnector flowStoreServiceConnector;

    public ConnectorFactory(String fileStoreServiceEndpoint, String jobStoreServiceEndpoint, String flowStoreServiceEndpoint)
            throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(fileStoreServiceEndpoint, "fileStoreServiceEndpoint");
        InvariantUtil.checkNotNullNotEmptyOrThrow(jobStoreServiceEndpoint, "jobStoreServiceEndpoint");
        InvariantUtil.checkNotNullNotEmptyOrThrow(flowStoreServiceEndpoint, "flowStoreServiceEndpoint");

        LOGGER.info("fileStoreServiceEndpoint: {}", fileStoreServiceEndpoint);
        LOGGER.info("jobStoreServiceEndpoint: {}", jobStoreServiceEndpoint);
        LOGGER.info("flowStoreServiceEndpoint: {}", flowStoreServiceEndpoint);

        final ClientConfig config = new ClientConfig();
        config.register(new JacksonFeature());
        config.property(ApacheClientProperties.REQUEST_CONFIG, RequestConfig.custom()
            .setConnectTimeout(5000)
            .setSocketTimeout(65000)
            .build());
        config.connectorProvider(new ApacheConnectorProvider());
        config.property(ClientProperties.CHUNKED_ENCODING_SIZE, 8 * 1024);
        client = HttpClient.newClient(config);
        fileStoreServiceConnector = new FileStoreServiceConnector(client, fileStoreServiceEndpoint);
        jobStoreServiceConnector = new JobStoreServiceConnector(client, jobStoreServiceEndpoint);
        flowStoreServiceConnector = new FlowStoreServiceConnector(client, flowStoreServiceEndpoint);
    }

    public FileStoreServiceConnector getFileStoreServiceConnector() {
        return fileStoreServiceConnector;
    }

    public JobStoreServiceConnector getJobStoreServiceConnector() {
        return jobStoreServiceConnector;
    }

    public FlowStoreServiceConnector getFlowStoreServiceConnector() {
        return flowStoreServiceConnector;
    }

    public void close() {
        HttpClient.closeClient(client);
    }
}
