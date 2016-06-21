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

import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.openagency.OpenAgencyConnector;

import javax.annotation.PostConstruct;
import javax.ejb.EJBException;
import javax.ejb.Singleton;
import javax.naming.NamingException;

/**
 * This Enterprise Java Bean (EJB) singleton is used as a connector
 * to the Open Agency SOAP based web service.
 */
@Singleton
public class OpenAgencyConnectorBean {
    OpenAgencyConnector openAgencyConnector;

    @PostConstruct
    public void initializeConnector() {
        try {
            final String endpoint = ServiceUtil.getOpenAgencyEndpoint();
            openAgencyConnector = new OpenAgencyConnector(endpoint);
        } catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    public OpenAgencyConnector getConnector() {
        return openAgencyConnector;
    }
}
