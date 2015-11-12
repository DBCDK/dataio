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
package dk.dbc.dataio.sink.openupdate;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.sink.openupdate.connector.OpenUpdateServiceConnector;
import dk.dbc.dataio.sink.types.SinkException;

import javax.ejb.EJB;
import javax.ejb.Singleton;

/**
 * This Enterprise Java Bean (EJB) singleton is used as a config container for the the Open Update web-service sink
 * to the Open Update web-service.
 */
@Singleton
public class OpenUpdateConfigBean {

    @EJB
    FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;

    private long highestVersionSeen = 0;
    private OpenUpdateServiceConnector openUpdateServiceConnector;

    /**
     *
     * @param consumedMessage the consumed message
     * @return configured openUpdateServiceConnector
     * @throws SinkException on error to retrieve property or sink
     */
    public OpenUpdateServiceConnector getConnector(ConsumedMessage consumedMessage) throws SinkException {
        configureConnector(consumedMessage);
        return openUpdateServiceConnector;
    }

    /*
     * Private methods
     */

    /**
     * This method determines if the current instance of the open update service connector is configured
     * with the latest version config values.
     *
     * If the current config is outdated:
     * The latest version of the config is retrieved through the referenced sink.
     * A new open update service connector is created with the new config values.

     * @param consumedMessage consumed message containing the version and the id of the sink containing the version
     * @throws SinkException on error to retrieve property for id or version or on error on fetching sink
     */
    private void configureConnector(ConsumedMessage consumedMessage) throws SinkException {
        try {
            final long sinkId = consumedMessage.getHeaderValue(JmsConstants.SINK_ID_PROPERTY_NAME, Long.class);
            final long sinkVersion = consumedMessage.getHeaderValue(JmsConstants.SINK_VERSION_PROPERTY_NAME, Long.class);

            if(sinkVersion > highestVersionSeen) {
                final Sink sink = flowStoreServiceConnectorBean.getConnector().getSink(sinkId);
                final OpenUpdateSinkConfig config = (OpenUpdateSinkConfig) sink.getContent().getSinkConfig();
                highestVersionSeen = sink.getVersion();
                openUpdateServiceConnector = new OpenUpdateServiceConnector(
                        config.getEndpoint(),
                        config.getUserId(),
                        config.getPassword());
            }
        } catch (FlowStoreServiceConnectorException e) {
            throw new SinkException(e.getMessage(), e);
        }
    }

}
