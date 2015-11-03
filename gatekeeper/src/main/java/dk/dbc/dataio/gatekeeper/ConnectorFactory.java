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

import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.client.Client;

public class ConnectorFactory {
    private final Client client;
    private final FileStoreServiceConnector fileStoreServiceConnector;
    private final JobStoreServiceConnector jobStoreServiceConnector;

    public ConnectorFactory(String fileStoreServiceEndpoint, String jobStoreServiceEndpoint)
            throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(fileStoreServiceEndpoint, "fileStoreServiceEndpoint");
        InvariantUtil.checkNotNullNotEmptyOrThrow(jobStoreServiceEndpoint, "jobStoreServiceEndpoint");

        final ClientConfig config = new ClientConfig();
        config.register(new JacksonFeature());
        config.connectorProvider(new ApacheConnectorProvider());
        config.property(ClientProperties.CHUNKED_ENCODING_SIZE, 8 * 1024);
        client = HttpClient.newClient(config);
        fileStoreServiceConnector = new FileStoreServiceConnector(client, fileStoreServiceEndpoint);
        jobStoreServiceConnector = new JobStoreServiceConnector(client, fileStoreServiceEndpoint);
    }

    public FileStoreServiceConnector getFileStoreServiceConnector() {
        return fileStoreServiceConnector;
    }

    public JobStoreServiceConnector getJobStoreServiceConnector() {
        return jobStoreServiceConnector;
    }

    public void close() {
        HttpClient.closeClient(client);
    }
}
