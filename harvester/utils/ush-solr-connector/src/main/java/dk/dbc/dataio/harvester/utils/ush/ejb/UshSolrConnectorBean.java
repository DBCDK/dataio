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

package dk.dbc.dataio.harvester.utils.ush.ejb;

import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.harvester.utils.ush.UshSolrConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.ejb.Singleton;
import javax.naming.NamingException;

/**
 * This Enterprise Java Bean (EJB) singleton is used to retrieve a connector to the USH Solr server.
 */
@Singleton
public class UshSolrConnectorBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(UshSolrConnectorBean.class);

    UshSolrConnector ushSolrConnector;

    @PostConstruct
    public void initializeConnector() {
        LOGGER.debug("Initializing connector");
        try {
            final String endpoint = ServiceUtil.getUshSolrEndpoint();
            ushSolrConnector = new UshSolrConnector(/* endpoint */);
            LOGGER.info("Using solr endpoint {}", endpoint);
        } catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    public UshSolrConnector getConnector() {
        return ushSolrConnector;
    }
}
