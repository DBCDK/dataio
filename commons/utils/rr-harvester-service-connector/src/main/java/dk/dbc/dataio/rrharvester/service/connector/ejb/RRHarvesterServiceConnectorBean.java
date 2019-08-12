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

package dk.dbc.dataio.rrharvester.service.connector.ejb;

import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnector;
import dk.dbc.httpclient.HttpClient;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ws.rs.client.Client;

// TODO: 10-07-19 replace EJB with @ApplicationScoped CDI producer

@Singleton
@LocalBean
public class RRHarvesterServiceConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(RRHarvesterServiceConnectorBean.class);

    HarvesterTaskServiceConnector harvesterTaskServiceConnector;

    @PostConstruct
    public void initializeConnector() {
        final Client client = HttpClient.newClient(
                new ClientConfig()
                        .register(new JacksonFeature()));
        final String endpoint = System.getenv("RAWREPO_HARVESTER_URL");
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new EJBException("RAWREPO_HARVESTER_URL must be set");
        }
        harvesterTaskServiceConnector = new HarvesterTaskServiceConnector(client, endpoint);
        LOGGER.info("Using service endpoint {}", endpoint);
    }

    public HarvesterTaskServiceConnector getConnector() {
        return harvesterTaskServiceConnector;
    }

    @PreDestroy
    public void tearDownConnector() {
        HttpClient.closeClient(harvesterTaskServiceConnector.getHttpClient());
    }
}
