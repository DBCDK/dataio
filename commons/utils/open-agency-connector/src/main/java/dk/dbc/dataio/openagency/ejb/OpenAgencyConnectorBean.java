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

package dk.dbc.dataio.openagency.ejb;

import dk.dbc.dataio.openagency.OpenAgencyConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.ejb.Singleton;

// TODO: 05-07-19 replace EJB with @ApplicationScoped CDI producer

@Singleton
public class OpenAgencyConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAgencyConnectorBean.class);

    private String endpoint;

    @PostConstruct
    public void initializeConnector() {
        endpoint = System.getenv("OPENAGENCY_URL");
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new EJBException("OPENAGENCY_URL must be set");
        }
        LOGGER.info("endpoint={}", endpoint);
    }

    /**
     * @return new {@link OpenAgencyConnector} instance
     */
    public OpenAgencyConnector getConnector() {
        return new OpenAgencyConnector(endpoint);
    }
}
